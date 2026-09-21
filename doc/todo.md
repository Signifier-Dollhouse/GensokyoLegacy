- furnitures such as book stack and wooden chairs with 3 or more pixel of height should be "block" for path finding. Review them based on their shape height.
- morichika will walk off ladder when climbing down
- marisa can pathfind through vertical trapdoors a block above ground but cannot walk through it.
Trapdoor facing X axis in open state can block movements in X axis but not Y axis
- path finding in marisa's house is generally unworkable. sometimes pathfinding fail for no reason, and marisa arrives in strange spot.
This is probably because the path is too long and character might end up targeting a position near enough, while being under the floor