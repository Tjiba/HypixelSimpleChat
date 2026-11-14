package com.simplechat;

import com.simplechat.engine.Channel;

/** Canal d'origine, indépendant du texte et des préfixes affichés. */
public interface IHscMessage {
    Channel hsc$getChannel();
    void hsc$setChannel(Channel channel);
}
