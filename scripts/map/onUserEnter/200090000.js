var mapId = 200090000;

function start(ms) {
    var map = ms.getClient().getChannelServer().getMapFactory().getMap(mapId);

    if (map.getDocked()) {
        const CField = Java.type('connection.packets.CField');
        ms.getClient().sendPacket(CField.musicChange("Bgm04/ArabPirate"));
        ms.getClient().sendPacket(CField.crogBoatPacket(true));
    }

    return (true);
}