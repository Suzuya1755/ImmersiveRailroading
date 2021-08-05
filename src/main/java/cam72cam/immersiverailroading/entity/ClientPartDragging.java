package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.components.ModelComponent;
import cam72cam.immersiverailroading.util.VecUtil;
import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.boundingbox.IBoundingBox;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.text.PlayerMessage;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientPartDragging {
    private EntityRollingStock stock = null;
    private ModelComponent component = null;
    private Vec3d lastDelta = null;

    public static void register() {
        ClientPartDragging dragger = new ClientPartDragging();
        Mouse.registerDragHandler(dragger::capture);
        ClientEvents.TICK.subscribe(dragger::tick);
        Packet.register(DragPacket::new, PacketDirection.ClientToServer);
    }

    private boolean capture(Player.Hand hand) {
        if (hand == Player.Hand.SECONDARY) {
            if (MinecraftClient.getEntityMouseOver() instanceof EntityRollingStock) {
                stock = (EntityRollingStock) MinecraftClient.getEntityMouseOver();
                List<ModelComponent> targets = stock.getDefinition().getModel().getDraggableComponents();
                Player player = MinecraftClient.getPlayer();

                float yaw = player.getRotationYaw() - stock.getRotationYaw();
                float pitch = player.getRotationPitch();
                double f = Math.cos(-yaw * 0.017453292F - (float)Math.PI);
                double f1 = Math.sin(-yaw * 0.017453292F - (float)Math.PI);
                double f2 = -Math.cos(-pitch * 0.017453292F);
                double f3 = Math.sin(-pitch * 0.017453292F);
                Vec3d look = VecUtil.rotateWrongYaw(new Vec3d(f1 * f2, f3, f * f2), 0);

                Vec3d starta = VecUtil.rotateWrongYaw(stock.getPosition().subtract(player.getPositionEyes()), 180-stock.getRotationYaw());
                Vec3d start = starta.add(0, -starta.y + player.getPositionEyes().y - stock.getPosition().y, 0);
                double padding = 0.05 * stock.gauge.scale();
                List<ModelComponent> found = targets.stream().filter(g -> {
                    IBoundingBox bb = IBoundingBox.from(g.min, g.max).grow(new Vec3d(padding, padding, padding));
                    for (double i = 0; i < 2; i+=0.1) {
                        if (bb.contains(start.add(look.scale(i * stock.gauge.scale())))) {
                            return true;
                        }
                    }
                    return false;
                }).sorted(Comparator.comparingDouble(g -> g.min.add(g.max).scale(0.5f).distanceTo(start))).collect(Collectors.toList());
                for (ModelComponent objGroup : found) {
                    player.sendMessage(PlayerMessage.direct(objGroup.type.name()));
                }
                if (!found.isEmpty()) {
                    component = found.get(0);
                    return false;
                } else {
                    stock = null;
                }
            }
        }
        return true;
    }

    public static class DragPacket extends Packet {
        @TagField
        private UUID stockUUID;
        @TagField
        private ModelComponentType type;
        @TagField
        private double x;
        @TagField
        private double y;

        public DragPacket() {
            super(); // Reflection
        }
        public DragPacket(EntityRollingStock stock, ModelComponentType type, double x, double y) {
            this.stockUUID = stock.getUUID();
            this.type = type;
            this.x = x;
            this.y = y;
        }
        @Override
        protected void handle() {
            EntityRollingStock stock = getWorld().getEntity(stockUUID, EntityRollingStock.class);
            stock.onDrag(type, x, y);
        }
    }

    private void tick() {
        if (stock != null) {
            Vec3d delta = Mouse.getDrag();
            if (delta == null) {
                stock = null;
                component = null;
                lastDelta = null;
                return;
            }
            if (lastDelta == null) {
                lastDelta = delta;
                return;
            }
            if (lastDelta.distanceTo(delta) < 10) {
                return;
            }
            //stock.onDrag(component, delta.x / 1000, delta.y / 1000);
            new DragPacket(stock, component.type, (delta.x - lastDelta.x) / 1000, (delta.y - lastDelta.y) / 1000).sendToServer();
            lastDelta = delta;
        }
    }
}
