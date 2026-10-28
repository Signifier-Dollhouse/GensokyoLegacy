package dev.xkmc.gensokyolegacy.content.attachment.character;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;

@SerialClass
public class PartyModuleData {

	/**
	 * Reputation below which a character will not accept a personal invitation.
	 */
	public static final int INVITE_MIN_REPUTATION = 50;

	/**
	 * Set by an invitation letter, read and cleared by the next party she joins.
	 */
	@SerialField
	private boolean invited;

	public boolean invited() {
		return invited;
	}

	public void invite() {
		invited = true;
	}

	public void consumeInvite() {
		invited = false;
	}

}
