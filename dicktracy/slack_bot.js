if (!process.env.token) {
    console.log('Error: Specify token in environment');
    process.exit(1);
}

var Botkit = require('./node_modules/botkit/lib/Botkit.js');
var os = require('os');
var child_process = require('child_process');
console.log(child_process);

var controller = Botkit.slackbot({
    debug: 0,
    json_file_store: '/tmp/dicktracydata.json'
});

var bot = controller.spawn({
    token: process.env.token
}).startRTM();

controller.hears(['déploie ([^ ]*) .*'], 'direct_message,direct_mention,mention', function (bot, message) {
    var branchName = message.match[1];
    bot.reply(message, 'C\'est parti! Je te pinguerai quand le déploiement de ' + branchName + " sera opé.");
    //var docker = child_process.spawn('./docker.sh', ['deploy-sw'], {cwd: './workspace/arthur/excalibur-rest'});
    var docker = child_process.spawn('ls', [], {cwd: './workspace/arthur/excalibur-rest'});
    controller.storage.users.save({id: message.user, foo:'bar'}, function(err) { console.log("error storing " + err);});
    docker.stdout.on('data', function (data) {
        console.log("stdout:" + data);
    });

    docker.stderr.on('data', function (data) {
        console.log("stderr:" + data);
    });

    docker.on('close', function (code) {
        console.log("docker exited with code:" + code);
        console.log(message);
        console.log(controller.storage.users.get(message.user, function (err, userdata) {
            console.log("in cb err " + err);
            console.log("in cb userdata " + userdata);
            console.log(userdata);
        }));
        console.log(controller.storage.users.all(function (err, userdata) {
            console.log("in all cb err " + err);
            console.log("in all cb userdata " + userdata);
            console.log(userdata);
        }));

        console.log(bot.api.users.list({}, function (err, res) {
            console.log("in all users  err " + err);
            console.log("in all users  userdata " + res);
            console.log(res);
        }));
        bot.reply(message, '@' + message.user + ' OK')
    });
});

child_process.execSync("./init-workspace.sh", function (error, stdout, stderr) {
    if (error) {
        console.error("exec error:" + error);
        return;
    }
    console.log("stdout:" + stdout);
    console.log("stderr:" + stderr);
});