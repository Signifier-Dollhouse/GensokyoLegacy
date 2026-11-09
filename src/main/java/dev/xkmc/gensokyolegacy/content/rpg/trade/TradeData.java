package dev.xkmc.gensokyolegacy.content.rpg.trade;

import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;

@SerialClass
public class TradeData {

	@SerialField
	public int trades;

	@SerialField
	public long timestamp;

}
