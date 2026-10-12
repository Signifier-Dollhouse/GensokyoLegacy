implement suika attack via spell card system.

attack moves:
1. when closest target is more than 12 blocks away: jump toward target via setDeltaMovement. Unlock speed limit during this move.
2. when closest target is 3 - 12 blocks away: pull all targets within 12 blocks with 聚集 animation
3. when closest target is within 3 blocks: swing mainhand and kockback all targets within 6 blocks via setDeltaMovement.

When suika is below 70% health:
move 1 also spawns circular ring of danmaku perpendicular to motion direction. Spawn 24 blue mentos with speed of 1
move 2 also spawns danmaku in the following way: initiate 3 ticker per tick, each ticker last for 40 ticks. Ticker initiate with 1 random vector (via normalized 3d gaussian), then pick a random angle: spawn danmaku on plane perpendicular to the vector, with provided angle and the angle rotate with period of 40 ticks. spawned danmaku will be 16 blocks away from suika, and move toward suika at speed of 1 (lifetime = 16 ticks).
move 3 also summon some danmaku per tick for 20 ticks, t=1~20, at 20*t random position on radius t/3. spawned on suika y, 2 danmaku per position, one moving up at speed 1, and one moving down at speed 1.