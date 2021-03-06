package eu32k.gdx.artemis.base;

import java.util.HashMap;
import java.util.Map;

import eu32k.gdx.artemis.base.utils.Bag;
import eu32k.gdx.artemis.base.utils.ImmutableBag;

/**
 * The primary instance for the framework. It contains all the managers.
 * 
 * You must use this to create, delete and retrieve entities.
 * 
 * It is also important to set the delta each game loop iteration, and initialize before game loop.
 * 
 * @author Arni Arent
 * 
 */
public class World {
   private EntityManager em;
   private ComponentManager cm;

   public float delta;
   private final Bag<Entity> added;
   private final Bag<Entity> changed;
   private final Bag<Entity> deleted;
   private final Bag<Entity> enable;
   private final Bag<Entity> disable;

   private final AddedPerformer addedPerformer;
   private final ChangedPerformer changedPerformer;
   private final DeletedPerformer deletedPerformer;
   private final EnabledPerformer enabledPerformer;
   private final DisabledPerformer disabledPerformer;

   private final Map<Class<? extends Manager>, Manager> managers;
   private final Bag<Manager> managersBag;

   private final Map<Class<?>, EntitySystem> systems;
   private final Bag<EntitySystem> systemsBag;

   public World() {
      managers = new HashMap<Class<? extends Manager>, Manager>();
      managersBag = new Bag<Manager>();

      systems = new HashMap<Class<?>, EntitySystem>();
      systemsBag = new Bag<EntitySystem>();

      added = new Bag<Entity>();
      changed = new Bag<Entity>();
      deleted = new Bag<Entity>();
      enable = new Bag<Entity>();
      disable = new Bag<Entity>();

      addedPerformer = new AddedPerformer();
      changedPerformer = new ChangedPerformer();
      deletedPerformer = new DeletedPerformer();
      enabledPerformer = new EnabledPerformer();
      disabledPerformer = new DisabledPerformer();

      cm = new ComponentManager();
      setManager(cm);

      em = new EntityManager();
      setManager(em);
   }

   /**
    * Makes sure all managers systems are initialized in the order they were added.
    */
   public void initialize() {
      for (int i = 0; i < managersBag.size(); i++) {
         managersBag.get(i).initialize();
      }

      for (int i = 0; i < systemsBag.size(); i++) {
         // ComponentMapperInitHelper.config(systemsBag.get(i), this);
         systemsBag.get(i).initialize();
      }
   }

   /**
    * Returns a manager that takes care of all the entities in the world.
    * entities of this world.
    * 
    * @return entity manager.
    */
   public EntityManager getEntityManager() {
      return em;
   }

   /**
    * Returns a manager that takes care of all the components in the world.
    * 
    * @return component manager.
    */
   public ComponentManager getComponentManager() {
      return cm;
   }

   /**
    * Add a manager into this world. It can be retrieved later.
    * World will notify this manager of changes to entity.
    * 
    * @param manager to be added
    */
   public <T extends Manager> T setManager(T manager) {
      managers.put(manager.getClass(), manager);
      managersBag.add(manager);
      manager.setWorld(this);
      return manager;
   }

   /**
    * Returns a manager of the specified type.
    * 
    * @param <T>
    * @param managerType
    *        class type of the manager
    * @return the manager
    */
   @SuppressWarnings("unchecked")
   public <T extends Manager> T getManager(Class<T> managerType) {
      return (T) managers.get(managerType);
   }

   /**
    * Deletes the manager from this world.
    * 
    * @param manager to delete.
    */
   public void deleteManager(Manager manager) {
      managers.remove(manager.getClass());
      managersBag.remove(manager);
   }

   /**
    * Time since last game loop.
    * 
    * @return delta time since last game loop.
    */
   public float getDelta() {
      return delta;
   }

   /**
    * You must specify the delta for the game here.
    * 
    * @param delta time since last game loop.
    */
   public void setDelta(float delta) {
      this.delta = delta;
   }

   /**
    * Adds a entity to this world.
    * 
    * @param e entity
    */
   public void addEntity(Entity e) {
      added.add(e);
   }

   /**
    * Ensure all systems are notified of changes to this entity.
    * If you're adding a component to an entity after it's been
    * added to the world, then you need to invoke this method.
    * 
    * @param e entity
    */
   public void changedEntity(Entity e) {
      changed.add(e);
   }

   /**
    * Delete the entity from the world.
    * 
    * @param e entity
    */
   public void deleteEntity(Entity e) {
      if (!deleted.contains(e)) {
         deleted.add(e);
      }
   }

   /**
    * (Re)enable the entity in the world, after it having being disabled.
    * Won't do anything unless it was already disabled.
    */
   public void enable(Entity e) {
      enable.add(e);
   }

   /**
    * Disable the entity from being processed. Won't delete it, it will
    * continue to exist but won't get processed.
    */
   public void disable(Entity e) {
      disable.add(e);
   }

   /**
    * Create and return a new or reused entity instance.
    * Will NOT add the entity to the world, use World.addEntity(Entity) for that.
    * 
    * @return entity
    */
   public Entity createEntity() {
      return em.createEntityInstance();
   }

   /**
    * Get a entity having the specified id.
    * 
    * @param entityId
    * @return entity
    */
   public Entity getEntity(int entityId) {
      return em.getEntity(entityId);
   }

   /**
    * Gives you all the systems in this world for possible iteration.
    * 
    * @return all entity systems in world.
    */
   public ImmutableBag<EntitySystem> getSystems() {
      return systemsBag;
   }

   /**
    * Adds a system to this world that will be processed by World.process()
    * 
    * @param system the system to add.
    * @return the added system.
    */
   public <T extends EntitySystem> T setSystem(T system) {
      return setSystem(system, false);
   }

   /**
    * Will add a system to this world.
    * 
    * @param system the system to add.
    * @param passive wether or not this system will be processed by World.process()
    * @return the added system.
    */
   public <T extends EntitySystem> T setSystem(T system, boolean passive) {
      system.setWorld(this);
      system.setPassive(passive);

      systems.put(system.getClass(), system);
      systemsBag.add(system);

      return system;
   }

   /**
    * Removed the specified system from the world.
    * 
    * @param system to be deleted from world.
    */
   public void deleteSystem(EntitySystem system) {
      systems.remove(system.getClass());
      systemsBag.remove(system);
   }

   private void notifySystems(Performer performer, Entity e) {
      for (int i = 0, s = systemsBag.size(); s > i; i++) {
         performer.perform(systemsBag.get(i), e);
      }
   }

   private void notifyManagers(Performer performer, Entity e) {
      for (int a = 0, s = managersBag.size(); s > a; a++) {
         performer.perform(managersBag.get(a), e);
      }
   }

   /**
    * Retrieve a system for specified system type.
    * 
    * @param type type of system.
    * @return instance of the system in this world.
    */
   @SuppressWarnings("unchecked")
   public <T extends EntitySystem> T getSystem(Class<T> type) {
      return (T) systems.get(type);
   }

   /**
    * Performs an action on each entity.
    * 
    * @param entityBag
    * @param performer
    */
   private void check(Bag<Entity> entityBag, Performer performer) {
      Object[] entities = entityBag.getData();
      for (int i = 0, s = entityBag.size(); s > i; i++) {
         Entity e = (Entity) entities[i];
         notifyManagers(performer, e);
         notifySystems(performer, e);
      }
      entityBag.clear();
   }

   /**
    * Process all non-passive systems.
    */
   public void process() {
      check(added, addedPerformer);
      check(changed, changedPerformer);
      check(disable, disabledPerformer);
      check(enable, enabledPerformer);
      check(deleted, deletedPerformer);

      cm.clean();

      for (int i = 0, s = systemsBag.size(); s > i; i++) {
         EntitySystem system = systemsBag.get(i);
         if (!system.isPassive()) {
            system.process();
         }
      }
   }

   /**
    * Retrieves a ComponentMapper instance for fast retrieval of components from entities.
    * 
    * @param type of component to get mapper for.
    * @return mapper for specified component type.
    */
   public <T extends Component> ComponentMapper<T> getMapper(Class<T> type) {
      return ComponentMapper.getFor(type, this);
   }

   // @SuppressWarnings("unchecked")
   // public <T extends Component> ComponentMapper<T> getMapper(String type) {
   // try
   // {
   // Class<T> klazz = (Class<T>)Class.forName(type);
   // return ComponentMapper.getFor(klazz, this);
   // }
   // catch (ClassNotFoundException e)
   // {
   // throw new RuntimeException(e);
   // }
   // }

   private final class DeletedPerformer implements Performer {
      @Override
      public void perform(EntityObserver observer, Entity e) {
         observer.deleted(e);
      }
   }

   private final class EnabledPerformer implements Performer {
      @Override
      public void perform(EntityObserver observer, Entity e) {
         observer.enabled(e);
      }
   }

   private final class DisabledPerformer implements Performer {
      @Override
      public void perform(EntityObserver observer, Entity e) {
         observer.disabled(e);
      }
   }

   private final class ChangedPerformer implements Performer {
      @Override
      public void perform(EntityObserver observer, Entity e) {
         observer.changed(e);
      }
   }

   private final class AddedPerformer implements Performer {
      @Override
      public void perform(EntityObserver observer, Entity e) {
         observer.added(e);
      }
   }

   /*
    * Only used internally to maintain clean code.
    */
   private interface Performer {
      void perform(EntityObserver observer, Entity e);
   }

   // private static class ComponentMapperInitHelper {
   //
   // public static void config(Object target, World world) {
   // try {
   // Class<?> clazz = target.getClass();
   // for (Field field : clazz.getDeclaredFields()) {
   // Mapper annotation = field.getAnnotation(Mapper.class);
   // if (annotation != null && Mapper.class.isAssignableFrom(Mapper.class)) {
   // ParameterizedType genericType = (ParameterizedType) field.getGenericType();
   // Class componentType = (Class) genericType.getActualTypeArguments()[0];
   //
   // field.setAccessible(true);
   // field.set(target, world.getMapper(componentType));
   // }
   // }
   // } catch (Exception e) {
   // throw new RuntimeException("Error while setting component mappers", e);
   // }
   // }
   //
   // }

}
