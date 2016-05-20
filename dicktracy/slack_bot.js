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

    var git_fetch = child_process.spawn('git', ['fetch']);
    git_fetch.on('close', function(code){
        if(code != 0) {
            bot.reply(message, "Désolé, j'ai un petit soucis avec git fetch");
        } else {
            var git_checkout = child_process.spawn('git', ['checkout', branchName]);
            git_checkout.on('close', function(code) {
                if(code != 0) {
                    bot.reply(message, "Désolé, j'ai un petit soucis pour checkouter " + branchName);
                } else {
                    bot.reply(message, 'C\'est parti! Je te pinguerai quand le déploiement de ' + branchName + " sera opé.");
                    var docker = child_process.spawn('./docker.sh', ['deploy-sw'], {cwd: './workspace/arthur/excalibur-rest'});
                    docker.stdout.on('data', function (data) {
                        console.log("stdout:" + data);
                    });
                    docker.stderr.on('data', function (data) {
                        console.log("stderr:" + data);
                    });
                    docker.on('close', function (code) {
                        console.log("docker exited with code:" + code);
                        if(code != 0) {
                            bot.reply(message, "Ca n'a pas fonctionné.");
                        } else {
                            bot.api.users.info({user: message.user}, function (err, res) {
                                console.log("err: " + err);
                                bot.reply(message, branchName + ' est déployée @' + res.user.name);
                            });
                        }
                    });
                }
            });
        }
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