package com.github.applejuiceyy.figuraextras.views.trees.core;

import com.github.applejuiceyy.figuraextras.FiguraExtras;
import com.github.applejuiceyy.figuraextras.util.Event;
import com.github.applejuiceyy.figuraextras.util.Observers;

import java.util.ArrayList;
import java.util.Optional;

public class ExpanderHost<V> {
    Expander.Callback callback;
    Event<Runnable>.Source ticker;
    Event<Runnable> stopUpdatingEntries = Event.runnable();

    public ExpanderHost(
            Observers.Observer<Optional<V>> observer,
            Registration registration,
            Expander.Callback callback,
            Event<Runnable>.Source ticker) {
        this.ticker = ticker;
        this.callback = callback;

        ExpanderAdder initialAdder = new ExpanderAdder(
                new ArrayList<>(),
                registration,
                this
        );
        initialAdder.add(observer);
    }

    public Runnable getCancel() {
        return () -> {
            FiguraExtras.logger.info("Now no longer ticking item listings");
            stopUpdatingEntries.getSink().run(Runnable::run);
        };
    }
}
