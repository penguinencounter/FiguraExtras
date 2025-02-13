package com.github.applejuiceyy.figuraextras.views.backend;


import com.github.applejuiceyy.figuraextras.ipc.backend.ReceptionistServerBackend;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.ParentElement;
import com.github.applejuiceyy.figuraextras.tech.gui.basics.Surface;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Button;
import com.github.applejuiceyy.figuraextras.tech.gui.elements.Spacer;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Flow;
import com.github.applejuiceyy.figuraextras.tech.gui.layout.Grid;
import com.github.applejuiceyy.figuraextras.util.Lifecycle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AvatarInstance implements Lifecycle {

    public AvatarInstance(ReceptionistServerBackend.BackendAvatar user, ParentElement.AdditionPoint additionPoint) {

        Flow root = new Flow();
        additionPoint.accept(root);

        root.setSurface(Surface.contextBackground());

        root.add(user.getId() + " by " + user.getOwner().toString() + " (getting deleted in " + ChronoUnit.HOURS.between(Instant.now(), user.getUpkeep().plus(Duration.ofDays(10))) + " hours)");

        ParentElement<Grid.GridSettings> deleteButton = Button.minimal().addAnd(Component.literal("Delete Avatar").withStyle(ChatFormatting.RED));
        deleteButton.activation.getSource().subscribe(evt -> user.delete());
        root.add(new Spacer(0, 10));
        root.add(deleteButton);
    }

    @Override
    public void tick() {

    }

    @Override
    public void render() {

    }

    public void dispose() {

    }
}
