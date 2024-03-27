package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Elements;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Label;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.network.chat.Component;

public class ErrorView implements Lifecycle {
    Element root;

    public ErrorView(Component text, ParentElement.AdditionPoint ip) {
        root = Elements.center(new Label(text));
    }

    @Override
    public void tick() {
    }

    @Override
    public void render() {
    }

    @Override
    public void dispose() {
    }
}
