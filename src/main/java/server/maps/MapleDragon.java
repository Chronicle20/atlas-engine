package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import connection.packets.CUser;


public class MapleDragon extends AbstractAnimatedMapleMapObject {

    private final MapleCharacter owner;

    public MapleDragon(MapleCharacter chr) {
        super();
        this.owner = chr;
        this.setPosition(chr.getPosition());
        this.setStance(chr.getStance());
        this.sendSpawnData(chr.getClient());
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.DRAGON;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.sendPacket(CUser.spawnDragon(this));
    }

    @Override
    public int getObjectId() {
        return owner.getId();
    }

    @Override
    public void sendDestroyData(MapleClient c) {
        c.sendPacket(CUser.removeDragon(owner.getId()));
    }

    public MapleCharacter getOwner() {
        return owner;
    }
}