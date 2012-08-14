package ru.catssoftware.gameserver.model.entity.events;


import org.apache.log4j.Logger;


import ru.catssoftware.gameserver.model.L2Attackable;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2ItemInstance;
import ru.catssoftware.gameserver.model.L2Skill;
import ru.catssoftware.gameserver.model.actor.instance.L2NpcInstance;
import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;

/**
 * 
 * @author Nick
 * Абстрактный класс, реализующий общий механиз эвентов<br>
 * Позволяет реализовывать как "военные" так и "мирные" эвенты
 */
public abstract class GameEvent
{
	protected static Logger _log = Logger.getLogger("Events");

	public enum State
	{
		STATE_OFFLINE("<font color=\"FF0000\">Offline</font>"),
		STATE_ACTIVE("<font color=\"008000\">Active</font>"),
		STATE_RUNNING("<font color=\"FFFF00\">Running</font>");

		private String status;

		State(String status)
		{
			this.status = status;
		}

		@Override
		public String toString()
		{
			return status;
		}
	}
	
	public static interface IGameEventScript {
		public void onStart(int instanceId);
		public void onFinish(int instanceId);
	}
	protected IGameEventScript _eventScript;
	public void setEventScript(IGameEventScript script) {
		_eventScript = script;
	}
	public int getRegistredPlayersCount() {
		return 0;
	}

	private State _state = State.STATE_OFFLINE;
	/**
	 * Получить текущее состояние эвента<br>
	 * У любого эвента должно быть минимум 2 состояния: STATE_INACTIVE и STATE_RUNNING<br>
	 * Состояние STATE_ACTIVE предназначено для эвентов, у которых есть период регистрации<br> 
	 * @return as int - состояние
	 */
	public boolean isState(State state)
	{
		return _state.equals(state);
	}

	public void setState(State state)
	{
		_state = state;
	}

	public State getState()
	{
		return _state;
	}

	/**
	 * Возврацает наименование эвента.<br>
	 * Это наименование используется в команде bypass event <i>имя</i> команда<br>
	 * Так же используется менеджером эвентов в параметрах автозапуска<br>
	 * (параметры <i>имя</i>.StartAt в main/mods/events.properties<br> 
	 * @return as String - наименование эвента
	 */
	abstract public String getName();
	
	/**
	 * Запущен ли эвент. По умолчанию да, если getState()==STATE_RUNNING<br>
	 * @return as boolean
	 */
	public boolean isRunning()
	{
		return isState(State.STATE_RUNNING);
	}

	/**
	 * Может ли игрок быть зарегистрирован на эвенте<br>
	 * @param player as L2PcInstance<br>
	 * @return as boolean
	 */
	public boolean canRegister(L2PcInstance player)
	{
		return isState(State.STATE_ACTIVE) && !isParticipant(player) && player.getGameEvent() == null;
	}
	
	/**
	 * Загрузка эвента. Вызывается автоматически GameEventManager<br>
	 * @return as boolean
	 */
	abstract public boolean load();

	/**
	 * Является ли игрок участником эвента<br> 
	 * @param player as L2PcInstance<br>
	 * @return as boolean
	 */
	abstract public boolean isParticipant(L2PcInstance player);
	
	/**
	 * Зарегистрировать игрока на эвенте<br>
	 * При регистрации ОБЯЗАТЕЛЬНО проставить player.getGameEvent() == this<br>
	 * @param player as L2PcInstance<br>
	 * @return as boolean - true если регистрация прошла успешно
	 */
	abstract public boolean register(L2PcInstance player);

	/**
	 * Удалить игрока с эвента.<br>
	 * При удалении ОБЯЗАТЕЛЬНО проставить player.getGameEvent() == null<br>
	 * @param player as L2PcInstance
	 */
	abstract public void remove(L2PcInstance player);
	
	/**
	 * Запуск эвента<br>
	 * Используется в механизме общего управления эвентами<br>
	 * @return as boolean - true если запуск прошел успешно
	 */
	abstract public boolean start();
	
	/**
	 * Останов эвента<br>
	 * Используется в механизме общего управления эвентами<br>
	 * @return as boolean - true если останов прошел успешно
	 */
	abstract public boolean finish();
	
	
	/**
	 * Может ли игрок, зарегистрированный на эвенте, взаимодействвать с другими объектами<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при actor.getGameEvent()!=null<br>
	 * @param actor as L2Character - общающийся<br>
	 * @param target as L2Character - с кем порываемся поговорить<br> 
	 * @return as boolean 
	 */
	public boolean canInteract(L2Character actor, L2Character target)
	{
		return true;
	}

	/**
	 * Может ли игрок, зарегистрированный на эвенте, атаковать цель<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при attacker.getGameEvent()!=null<br>
	 * @param attacker as L2Character - атакующий<br>
	 * @param target as L2Character - цель<br> 
	 * @return as boolean 
	 */
	public boolean canAttack(L2Character attacker, L2Character target)
	{
		return true;
	}
	

	/**
	 * Может ли цель, быть целью для масс-скила<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при caster.getGameEvent()!=null<br>
	 * @param caster as L2Character - кастующий<br>
	 * @param target as L2Character - цель<br>
	 * @param skill as L2Skill - используемый скилл<br> 
	 * @return as boolean 
	 */
	public boolean canBeSkillTarget(L2Character caster, L2Character target, L2Skill skill)
	{
		return true;
	}

	/**
	 * Можно ли использовать предмет на эвенте<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при actor.getGameEvent()!=null<br>
	 * @param actor as L2Character - использующий<br>
	 * @param item  as L2ItemInstance - используемый предмет<br>
	 * @return as boolean
	 */
	public boolean canUseItem(L2Character actor, L2ItemInstance item)
	{
		return true;
	}
	
	/**
	 * Можно ли использовать скилл на эвенте<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при caster.getGameEvent()!=null<br>
	 * Сначала вызывается canUseSkill() если он вернул true, и скилл масстовый<br>
	 * то вызывается canBeSkillTarget()<br>
	 * @param caster as L2Character - кастующий<br>
	 * @param skill as L2Skill - используемый скилл<br>
	 * @return as boolean
	 */
	public boolean canUseSkill(L2Character caster, L2Skill skill)
	{
		return true;
	}
	
	/**
	 * Может ли быть выполено социальное действие<br>
	 * Проверяйте getState() сами, данный метод вызывается безусловно при player.getGameEvent()!=null<br>
	 * Действия прописаны (частично) как именованные константы в RequestSocialAction<br>
	 * @param player as L2PcInstance - игрок<br>
	 * @param action as int - действие<br>
	 * @return as boolean
	 */
	public boolean canDoAction(L2PcInstance player, int action)
	{
		return true;
	}
	
	/**
	 * Если у экземпляра L2NpcInstance _event!=null, то onNPCTalk() вызывается в методе<br>
	 * onAction()<br>
	 * @param npc as L2NpcInstance - непися<br>
	 * @param talker as L2PcInstance - общающийся
	 */
	abstract public boolean onNPCTalk(L2NpcInstance npc, L2PcInstance talker);
	
	/**
	 * Обработка комманды bypass <b>event</b> <i>имя команда параметр</i><br>
	 * @param actor as L2PcInstance - игрок, вызываший bypass<br>
	 * @param command as String - команда<br> 
	 * @param params as String - параметры
	 */
	abstract public void onCommand(L2PcInstance actor, String command, String params);

	/**
	 * Метод вызывается при входе персонажа, зарегестрированного на эвенте, в мир<br>
	 * При вызове уже player.getGameEvent() == this<br>
	 * @param player as L2PcInstance
	 */
	public void onLogin(L2PcInstance player)
	{
	}
	
	/**
	 * Вызывается, при воскрешении персонажа.<br> 
	 * Проверяйте getState() сами, данный метод вызывается безусловно при actor.getGameEvent()!=null<br>
	 * @param actor as L2PcInstance
	 */
	public void onRevive(L2Character actor)
	{
	}
	
	/**
	 * Вызывается при отсоединениии персонажа<br>
	 * @param player as L2PcInstance
	 */
	public void onLogout(L2PcInstance player)
	{
	}
	
	/**
	 * Вызывается при убийстве участника или участником эвента.
	 * Проверяйте getState() сами, данный метод вызывается безусловно при victim.getGameEvent()!=null || killer._victim!=null<br>
	 * @param killer as L2Character - убийца<br>
	 * @param victim as L2Character - жертва
	 */
	abstract public void onKill(L2Character killer, L2Character victim);
	
	/**
	 * Разрешать кнопки "В город", "В кх" при смерти<br>
	 * @param player as L2PcInstance - покойный<br>
	 * @return as boolean
	 */
	public boolean canTeleportOnDie(L2PcInstance player)
	{
		return !isRunning();
	}

	/**
	 * Разрешить при смерти потерю опыта<br>
	 * @return as boolean
	 */
	public boolean canLostExpOnDie()
	{
		return !isRunning();
	}

	public boolean canGaveExp(L2Attackable victim) {
		return true;
	}

	public int getCharNameColor(L2PcInstance cha, L2PcInstance other) {
		return cha.getNameColor();
	}

	public String getTitle(L2PcInstance cha, L2PcInstance other) {
		return cha.getTitle();
	}

	public String getName(L2PcInstance cha, L2PcInstance other) {
		return cha.getName();
	}

	public int getCharTitleColor(L2PcInstance cha, L2PcInstance other) {
		return cha.getTitleColor();
	}
	
	public boolean requestRevive(L2PcInstance cha, int _requestedPointType) {
		return false;
	}

	public boolean canLogout(L2PcInstance player) {
		return false;
	}

	public boolean canDropItems(L2Attackable victim, L2PcInstance killer) {
		return true;
	}

	public void onSkillHit(L2Character caster, L2Character target, L2Skill skill) {
		
	}
}
