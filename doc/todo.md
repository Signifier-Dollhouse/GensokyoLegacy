- create and some_assembly_required compat should be restored.
  - reference modular golems for adding create dep in gradle, and fetch SAR from curseforge with file version 7658719
- verify if any event has phase (PRE/POST) that we are supposed to check for, primarily tick event.
- mixin changes (interface, redirect, attribute)
  - ILivingEntityExtension interface mixin: override the method in LivingEntity and mixin the method (so that it doesn't conflict with other mods doing the same)
  - gravity attribute get value in travel: check if the attribute instance is indeed gravity.
  - never use overwrite or redirect
- Why CodecRegistry not using vanilla registry anymore?

Debug and testing focus:
- item container slot check if dupe could be possible
- block interaction
- capability persistence and sync
- item data component