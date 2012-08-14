package ru.catssoftware.extension;

public abstract class ObjectExtension implements IExtension {
	public static enum Action {
		ITEM_EQUIP,
		ITEM_UNEQUIP,
		ITEM_DESTROY,
		ITEM_CREATE, // process, actor, reference
		ITEM_USE,
		ITEM_AUGMENT,
		ITEM_SETOWNER, // process, new_owner_id, old_owner_id
		ITEM_ENCHANTSTART,
		ITEM_ENCHANTCALCCHANCE,
		ITEM_ENCHANTSUCCESS,
		ITEM_ENCHANTFAIL,
		CHAR_SPAWN,
		CHAR_DESPAWN,
		CHAR_INTENTIONCHANGE, // CtrlIntention, arg0, arg1
		CHAR_ATTACK, // target
		CHAR_CAST, // skill
		CHAR_DIE, // killer
		CHAR_REVIVE,
		CHAR_ENTERWORLD,
		CHAR_LEAVEWORLD,
		NPC_ONACTION, // player
		PC_SOCIALACTION,
		PC_CALCEXP, // exp
		PC_CALCDROP,
		PC_CALCSPOIL,
		PC_CALCSP, // sp
		PC_QUEST_FINISHED,
		PC_CHECK_SWEEP,
		PC_OLY_BATTLE_FINISHED,
		PC_LEARN_SKILL,
		PC_CHANGE_CLASS,
		PC_LEVEL_UP,
		DUEL_START,
		DUEL_FINISH,
		SKILL_SUCCESS,
		NPC_CAN_TEACH, // skill, player
		NPC_SHIFT_CLICK,
		ACC_SETNAME,
		QUEST_CALCREWARD // Quest, itemId, count
	}
	public abstract Class<?> [] forClasses();
	
	abstract public Object hanlde(Object object, Action action, Object...params );
	@Override
	public boolean load() {
		 return true;
	 }

	@Override
	public void init() { }
	
	@Override
	public boolean install(boolean paramBoolean)
	    throws Exception {
		 return false;
	}
	protected  static ExtensionInfo _dummyInfo = new ExtensionInfo() {

		@Override
		public String getName() {
			return "ObjectExcension";
		}

		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public boolean installRequired() {
			return false;
		}
		 
	 };
	 @Override
	 public ExtensionInfo getInfo() {
		  return _dummyInfo; 
	  }
	
}
