package ru.catssoftware.gameserver.model.zone;

import java.util.List;

import javolution.util.FastList;

import org.w3c.dom.Node;

import ru.catssoftware.gameserver.datatables.SkillTable;
import ru.catssoftware.gameserver.model.IEffector;
import ru.catssoftware.gameserver.model.L2Character;
import ru.catssoftware.gameserver.model.L2Skill;

public class L2BuffZone extends L2DefaultZone implements IEffector {

	private List<L2Skill> _commonBuffs = new FastList<L2Skill>();
	private List<L2Skill> _mageBuffs = new FastList<L2Skill>();
	private List<L2Skill> _warriorBuff = new FastList<L2Skill>();
	private void buffMe(L2Character cha) {
		for(L2Skill sk : _commonBuffs)
			sk.getEffects(cha, cha,this);
		if(cha.getActingPlayer().getClassId().isMage())
			for(L2Skill sk : _mageBuffs)
				sk.getEffects(cha, cha,this);
		else 
			for(L2Skill sk : _warriorBuff)
				sk.getEffects(cha, cha,this);
	}
	@Override
	protected void onEnter(L2Character cha) {
		if(cha.getActingPlayer()!=null) 
			buffMe(cha);
		super.onEnter(cha);

	}

	@Override
	protected void onExit(L2Character character) {
		super.onExit(character);

	}

	@Override
	public void onDieInside(L2Character character) {
		super.onDieInside(character);
	}

	@Override
	public void onReviveInside(L2Character cha) {
		if(cha.getActingPlayer()!=null) 
			buffMe(cha);
		super.onReviveInside(cha);
	}
	private void parseSkills(String skills, List<L2Skill> list) {
		String sk[] = skills.split(";");
		for(String s : sk) try {
			String info [] = s.split("-");
			if(info.length==2) {
				L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(info[0]), Integer.parseInt(info[1]));
				if(skill!=null)
					list.add(skill);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	protected void parseZoneDetails(Node zn) {
		for(Node n = zn.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeName().equals("buffs")) {
				for(Node n1 = n.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
					if(n1.getNodeName().equals("common")) 
						parseSkills(n1.getAttributes().getNamedItem("skills").getNodeValue(),_commonBuffs);
					else if(n1.getNodeName().equals("mage")) 
						parseSkills(n1.getAttributes().getNamedItem("skills").getNodeValue(),_commonBuffs);
					else if(n1.getNodeName().equals("warrior")) 
						parseSkills(n.getAttributes().getNamedItem("skills").getNodeValue(),_commonBuffs);
				}
			}
			
		}
	}
	@Override
	public void onEffectFinished(L2Character effected, L2Skill skill) {
		if(effected.getActingPlayer() == effected && !effected.isDead())
			if(_commonBuffs.contains(skill) || _mageBuffs.contains(skill) || _warriorBuff.contains(skill))
				skill.getEffects(effected,effected,this);
		
	}

}
