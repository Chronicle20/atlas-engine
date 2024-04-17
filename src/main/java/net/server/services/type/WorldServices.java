package net.server.services.type;

import net.server.services.BaseService;
import net.server.services.Service;
import net.server.services.ServiceType;
import net.server.services.task.world.CharacterSaveService;

public enum WorldServices implements ServiceType {

   SAVE_CHARACTER(CharacterSaveService.class);

   private final Class<? extends BaseService> s;

   WorldServices(Class<? extends BaseService> service) {
      s = service;
   }

   @Override
   public Service createService() {
      return new Service(s);
   }

   @Override
   public WorldServices[] enumValues() {
      return WorldServices.values();
   }
}
