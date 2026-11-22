package dev.xkmc.gensokyolegacy.content.rpg.trade;

/**
 * What a trade offer does: sell the result for currency, pay currency for ingredients,
 * or process ingredients into the result, in the order of the filter buttons.
 *
 * @see "doc/design/trade_ui.md"
 */
public enum OfferKind {
	TRADE,
	BUYING,
	PROCESSING
}
