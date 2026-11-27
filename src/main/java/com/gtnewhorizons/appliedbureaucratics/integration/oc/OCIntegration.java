package com.gtnewhorizons.appliedbureaucratics.integration.oc;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.gtnewhorizons.appliedbureaucratics.AppliedBureaucratics;
import com.gtnewhorizons.appliedbureaucratics.block.ModBlocks;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import li.cil.oc.api.Driver;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Items;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.internal.Robot;

/*-
Note [Robots] (generated)
~~~~~~~~~~~~~~~~~~~~~~~~~
At the Internet tier, a linked Mainframe takes OpenComputers robots in place of villagers (design document,
section 6.3, item 4).  A robot works when:

  - it stands on a Docking Pad (TileDockingPad) that is on the Mainframe's terminal network, and
  - it has our Requisition Upgrade (DriverRequisitionUpgrade), whose component `requisition` it calls `work()` on,
    the last time less than BotRegistry.WINDOW ticks ago.  The `workerd` loot floppy does exactly that.

MTEMainframe's office survey finds the pads among its concentrator's network nodes and the robot in the block above
each.  A tier 1 / 2 / 3 robot is 1.5 / 2 / 3 FTE.  A Mainframe's robots are one org unit, the botnet, worth the sum of
its robots times B: 0.5 ("half of all traffic is bots talking to bots") plus 0.0625 per contracted villager seated
within 2 blocks of a pad as a CAPTCHA solver, up to 1.0.  Robots count against the 32 seats and the hardware.  A
working robot on a pad cannot move off it (onRobotMove cancels RobotMoveEvent.Pre), so it is not walked away mid-shift.
*/

/// What we register with OpenComputers. See Note [Robots].
public final class OCIntegration {

    private OCIntegration() {}

    public static void init() {
        Driver.add(new DriverRequisitionUpgrade());
        Driver.add(new DriverFacility());
        Items.registerFloppy(
            "workerd",
            10,
            () -> FileSystem.fromClass(AppliedBureaucratics.class, AppliedBureaucratics.MODID, "loot/workerd"));
        MinecraftForge.EVENT_BUS.register(new OCIntegration.Events());
    }

    public static final class Events {

        @SubscribeEvent
        public void onRobotMove(final RobotMoveEvent.Pre event) {
            if (!(event.agent instanceof Robot robot)) {
                return;
            }
            final World world = robot.world();
            final int x = MathHelper.floor_double(robot.xPosition());
            final int y = MathHelper.floor_double(robot.yPosition());
            final int z = MathHelper.floor_double(robot.zPosition());
            if (world != null && world.getBlock(x, y - 1, z) == ModBlocks.dockingPad
                && BotRegistry.isWorking(world, x, y, z)) {
                event.setCanceled(true);
            }
        }
    }

    /// The robot standing on the pad at `x, y, z`, if it is working.
    public static Robot workingRobotOn(final World world, final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(x, y + 1, z);
        return te instanceof Robot robot && BotRegistry.isWorking(world, x, y + 1, z) ? robot : null;
    }
}
