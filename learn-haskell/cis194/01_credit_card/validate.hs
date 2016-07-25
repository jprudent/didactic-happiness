{-# OPTIONS_GHC -Wall #-}
module Main where

import           System.Exit (exitFailure)

doubleFirstDigit :: [Integer] -> [Integer]
doubleFirstDigit [] = []
doubleFirstDigit [x] = [x * 2]
doubleFirstDigit (x : (y : xs)) = x * 2 : (y : doubleFirstDigit xs)

doubleSecondDigit :: [Integer] -> [Integer]
doubleSecondDigit xs = reverse (doubleFirstDigit (reverse xs))

main :: IO ()
main = do
    putStrLn "This test always fails!"
    exitFailure
