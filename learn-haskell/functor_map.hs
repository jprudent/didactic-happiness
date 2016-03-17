import qualified Data.Map as Map

instance Functor (Map.Map k) where
  fmap f (Map.fromList l) = map (snd . f) l 
  fmap _ _ = Map.fromList []

newYearBonus :: (String a, Num b) => Map.Map a b -> Map.Map a b
newYearBonus salaries = fmap salaries (*1.1)
 
