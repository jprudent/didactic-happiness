import Network
import System.IO

server  = "irc.root-me.org"
port = 6667 

main = do
  h <- connectTo server (PortNumber (fromIntegral port))
  hSetBuffering h NoBuffering
  t <- hGetContents h
  print t


  
