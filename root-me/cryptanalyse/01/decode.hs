main = do
  encoded <- getLine
  putStrLn (asciiDecode encoded)

asciiDecode :: String -> String
asciiDecode s = s
