package com.tonic.api.widgets;

import com.tonic.data.EmoteID;

/**
 * Emote automation api
 */
public class EmoteAPI
{
    /**
     * perform an emote by id
     * @param emoteId emote id
     */
    public static void perform(int emoteId) {
        WidgetAPI.interact(1, EmoteID.EMOTES_WIDGET_ID, emoteId);
    }

    /**
     * perform an emote by EmoteID
     * @param emoteId emote id
     */
    public static void perform(EmoteID emoteId) {
        WidgetAPI.interact(1, EmoteID.EMOTES_WIDGET_ID, emoteId.getId());
    }
}
