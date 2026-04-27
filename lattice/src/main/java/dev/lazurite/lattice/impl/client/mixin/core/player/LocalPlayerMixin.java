package dev.lazurite.lattice.impl.client.mixin.core.player;

import dev.lazurite.lattice.api.point.ViewPoint;
import dev.lazurite.lattice.impl.ViewPointHelper;
import dev.lazurite.lattice.impl.api.player.InternalLatticeLocalPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements InternalLatticeLocalPlayer {

    @Unique
    private int viewPointEntityId = -1;

    @Unique
    private ViewPoint viewPoint;

    @Override
    public ViewPoint getViewPoint() {
        if (this.viewPoint == null) {
            this.viewPoint = (ViewPoint) (Object) this;
        }
        return this.viewPoint;
    }

    @Override
    public void setViewPointEntityId(final int id) {
        this.viewPointEntityId = id;
    }

    @Override
    public int getViewPointEntityId() {
        return this.viewPointEntityId;
    }

    @Override
    public void setViewPoint(final ViewPoint viewPoint) {
        this.viewPoint = viewPoint;
    }

}
