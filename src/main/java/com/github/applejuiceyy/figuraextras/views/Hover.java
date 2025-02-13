package com.github.applejuiceyy.figuraextras.views;

import com.github.applejuiceyy.figuraextras.tech.gui.basics.Element;
import com.github.applejuiceyy.figuraextras.util.Observers;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Hover {
    static final private Observers.WritableObserver<Object> currentHoverWritable = Observers.of(null);
    static final public Observers.Observer<Object> currentHover = currentHoverWritable;

    public static Runnable setHovering(Object obj) {
        currentHoverWritable.set(obj);
        return () -> {
            if (currentHoverWritable.get() == obj) {
                currentHoverWritable.set(null);
            }
        };
    }

    public static void elementHoverObject(Element element, Supplier<Object> object) {
        element.hoveringKind.observe(new Consumer<>() {
            Runnable toRun = null;

            @Override
            public void accept(Element.HoverIntent bool) {
                if (bool.look) {
                    toRun = setHovering(object.get());
                } else {
                    if (toRun == null) return;
                    toRun.run();
                    toRun = null;
                }
            }
        });
    }
}
