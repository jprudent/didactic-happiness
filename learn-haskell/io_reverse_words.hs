main = do
  line <- getLine
  if null line 
    then return ()
    else do 
      putStrLn $ reverseWords line
      main
  
reverseWords :: String -> String
reverseWords s = unwords $ map reverse $ words s
