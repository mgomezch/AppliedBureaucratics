package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizons.appliedbureaucratics.facility.IComputeFacility;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.DriverSidedTileEntity;

/// OpenComputers' driver for every facility, through an Adapter beside its controller or tray, as OC exposes the
/// Lapotronic Supercapacitor. The component is `ab_facility`. Lua users' edge is moving FTE between planning and
/// dispatch at the right moments (`setAllocation`) and deciding when people crunch (`setCrunch`): no hidden bonus.
public class DriverFacility extends DriverSidedTileEntity {

    @Override
    public Class<?> getTileEntityClass() {
        return TileEntity.class;
    }

    private static @Nullable IComputeFacility facility(final World world, final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IComputeFacility facility) {
            return facility;
        }
        if (te instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() instanceof IComputeFacility facility) {
            return facility;
        }
        return null;
    }

    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z, final ForgeDirection side) {
        return facility(world, x, y, z) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z,
        final ForgeDirection side) {
        final IComputeFacility facility = facility(world, x, y, z);
        return facility == null ? null : new Environment(facility);
    }

    public static final class Environment extends li.cil.oc.api.prefab.ManagedEnvironment {

        private final IComputeFacility facility;

        Environment(final IComputeFacility facility) {
            this.facility = facility;
            this.setNode(
                Network.newNode(this, Visibility.Network)
                    .withComponent("ab_facility")
                    .create());
        }

        @Callback(doc = "function():table -- The facility's status lines: the bottleneck first, then the figures.")
        public Object[] status(final Context context, final Arguments args) {
            return new Object[] { this.facility.statusLines()
                .toArray(new String[0]) };
        }

        @Callback(doc = "function():number -- Planning steps per second the facility gives its network.")
        public Object[] planningRate(final Context context, final Arguments args) {
            return new Object[] { this.facility.getPlanningRate() };
        }

        @Callback(
            doc = "function([share:number]) -- Pin the share of FTE spent planning (0 to 1), or nil for automatic.")
        public Object[] setAllocation(final Context context, final Arguments args) {
            this.facility.setPinnedPlanningShare(
                args.count() > 0 && !args.isString(0) && args.checkAny(0) != null ? args.checkDouble(0) : null);
            return new Object[] { true };
        }

        @Callback(doc = "function([crunch:boolean]) -- Force crunch on or off, or nil to follow redstone.")
        public Object[] setCrunch(final Context context, final Arguments args) {
            this.facility.setCrunchOverride(args.count() > 0 && args.checkAny(0) != null ? args.checkBoolean(0) : null);
            return new Object[] { true };
        }
    }
}
