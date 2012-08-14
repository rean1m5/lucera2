/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.catssoftware.gameserver.model.base;

/**
 * @author luisantonioa
 */
import static ru.catssoftware.gameserver.model.base.ClassLevel.First;
import static ru.catssoftware.gameserver.model.base.ClassLevel.Fourth;
import static ru.catssoftware.gameserver.model.base.ClassLevel.Second;
import static ru.catssoftware.gameserver.model.base.ClassLevel.Third;
import static ru.catssoftware.gameserver.model.base.ClassType.Fighter;
import static ru.catssoftware.gameserver.model.base.ClassType.Mystic;
import static ru.catssoftware.gameserver.model.base.ClassType.Priest;
import static ru.catssoftware.gameserver.model.base.Race.Darkelf;
import static ru.catssoftware.gameserver.model.base.Race.Dwarf;
import static ru.catssoftware.gameserver.model.base.Race.Elf;
import static ru.catssoftware.gameserver.model.base.Race.Human;
import static ru.catssoftware.gameserver.model.base.Race.Orc;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import ru.catssoftware.gameserver.model.actor.instance.L2PcInstance;


/**
 * This class ...
 *
 * @version $Revision: 1.2 $ $Date: 2004/06/27 08:12:59 $
 */
public enum PlayerClass
{
	humanFighter(Human, Fighter, First),
	Warrior(Human, Fighter, Second),
	Gladiator(Human, Fighter, Third),
	Warlord(Human, Fighter, Third),
	humanKnight(Human, Fighter, Second),
	Paladin(Human, Fighter, Third),
	DarkAvenger(Human, Fighter, Third),
	Rogue(Human, Fighter, Second),
	TreasureHunter(Human, Fighter, Third),
	Hawkeye(Human, Fighter, Third),
	humanMystic(Human, Mystic, First),
	humanWizard(Human, Mystic, Second),
	Sorceror(Human, Mystic, Third),
	Necromancer(Human, Mystic, Third),
	Warlock(Human, Mystic, Third),
	Cleric(Human, Priest, Second),
	Bishop(Human, Priest, Third),
	Prophet(Human, Priest, Third),

	ElvenFighter(Elf, Fighter, First),
	ElvenKnight(Elf, Fighter, Second),
	TempleKnight(Elf, Fighter, Third),
	Swordsinger(Elf, Fighter, Third),
	ElvenScout(Elf, Fighter, Second),
	Plainswalker(Elf, Fighter, Third),
	SilverRanger(Elf, Fighter, Third),
	ElvenMystic(Elf, Mystic, First),
	ElvenWizard(Elf, Mystic, Second),
	Spellsinger(Elf, Mystic, Third),
	ElementalSummoner(Elf, Mystic, Third),
	ElvenOracle(Elf, Priest, Second),
	ElvenElder(Elf, Priest, Third),

	DarkElvenFighter(Darkelf, Fighter, First),
	PalusKnight(Darkelf, Fighter, Second),
	ShillienKnight(Darkelf, Fighter, Third),
	Bladedancer(Darkelf, Fighter, Third),
	Assassin(Darkelf, Fighter, Second),
	AbyssWalker(Darkelf, Fighter, Third),
	PhantomRanger(Darkelf, Fighter, Third),
	DarkElvenMystic(Darkelf, Mystic, First),
	DarkElvenWizard(Darkelf, Mystic, Second),
	Spellhowler(Darkelf, Mystic, Third),
	PhantomSummoner(Darkelf, Mystic, Third),
	ShillienOracle(Darkelf, Priest, Second),
	ShillienElder(Darkelf, Priest, Third),

	orcFighter(Orc, Fighter, First),
	orcRaider(Orc, Fighter, Second),
	Destroyer(Orc, Fighter, Third),
	orcMonk(Orc, Fighter, Second),
	Tyrant(Orc, Fighter, Third),
	orcMystic(Orc, Mystic, First),
	orcShaman(Orc, Mystic, Second),
	Overlord(Orc, Mystic, Third),
	Warcryer(Orc, Mystic, Third),

	DwarvenFighter(Dwarf, Fighter, First),
	DwarvenScavenger(Dwarf, Fighter, Second),
	BountyHunter(Dwarf, Fighter, Third),
	DwarvenArtisan(Dwarf, Fighter, Second),
	Warsmith(Dwarf, Fighter, Third),

	dummyEntry1(null, null, null),
	dummyEntry2(null, null, null),
	dummyEntry3(null, null, null),
	dummyEntry4(null, null, null),
	dummyEntry5(null, null, null),
	dummyEntry6(null, null, null),
	dummyEntry7(null, null, null),
	dummyEntry8(null, null, null),
	dummyEntry9(null, null, null),
	dummyEntry10(null, null, null),
	dummyEntry11(null, null, null),
	dummyEntry12(null, null, null),
	dummyEntry13(null, null, null),
	dummyEntry14(null, null, null),
	dummyEntry15(null, null, null),
	dummyEntry16(null, null, null),
	dummyEntry17(null, null, null),
	dummyEntry18(null, null, null),
	dummyEntry19(null, null, null),
	dummyEntry20(null, null, null),
	dummyEntry21(null, null, null),
	dummyEntry22(null, null, null),
	dummyEntry23(null, null, null),
	dummyEntry24(null, null, null),
	dummyEntry25(null, null, null),
	dummyEntry26(null, null, null),
	dummyEntry27(null, null, null),
	dummyEntry28(null, null, null),
	dummyEntry29(null, null, null),
	dummyEntry30(null, null, null),
	dummyEntry31(null, null, null),
	dummyEntry32(null, null, null),
	dummyEntry33(null, null, null),
	dummyEntry34(null, null, null),

	/*
	 * (3rd classes)
	 */
	duelist(Human, Fighter, Fourth),
	dreadnought(Human, Fighter, Fourth),
	phoenixKnight(Human, Fighter, Fourth),
	hellKnight(Human, Fighter, Fourth),
	sagittarius(Human, Fighter, Fourth),
	adventurer(Human, Fighter, Fourth),
	archmage(Human, Mystic, Fourth),
	soultaker(Human, Mystic, Fourth),
	arcanaLord(Human, Mystic, Fourth),
	cardinal(Human, Mystic, Fourth),
	hierophant(Human, Mystic, Fourth),

	evaTemplar(Elf, Fighter, Fourth),
	swordMuse(Elf, Fighter, Fourth),
	windRider(Elf, Fighter, Fourth),
	moonlightSentinel(Elf, Fighter, Fourth),
	mysticMuse(Elf, Mystic, Fourth),
	elementalMaster(Elf, Mystic, Fourth),
	evaSaint(Elf, Mystic, Fourth),

	shillienTemplar(Darkelf, Fighter, Fourth),
	spectralDancer(Darkelf, Fighter, Fourth),
	ghostHunter(Darkelf, Fighter, Fourth),
	ghostSentinel(Darkelf, Fighter, Fourth),
	stormScreamer(Darkelf, Mystic, Fourth),
	spectralMaster(Darkelf, Mystic, Fourth),
	shillienSaint(Darkelf, Mystic, Fourth),

	titan(Orc, Fighter, Fourth),
	grandKhauatari(Orc, Fighter, Fourth),
	dominator(Orc, Mystic, Fourth),
	doomcryer(Orc, Mystic, Fourth),

	fortuneSeeker(Dwarf, Fighter, Fourth),
	maestro(Dwarf, Fighter, Fourth);


	private Race												_race;
	private ClassLevel											_level;
	private ClassType											_type;

	private static final Set<PlayerClass>						mainSubclassSet;
	private static final Set<PlayerClass>						neverSubclassed	= EnumSet.of(Overlord, Warsmith);

	private static final Set<PlayerClass>						subclasseSet1	= EnumSet.of(DarkAvenger, Paladin, TempleKnight, ShillienKnight);
	private static final Set<PlayerClass>						subclasseSet2	= EnumSet.of(TreasureHunter, AbyssWalker, Plainswalker);
	private static final Set<PlayerClass>						subclasseSet3	= EnumSet.of(Hawkeye, SilverRanger, PhantomRanger);
	private static final Set<PlayerClass>						subclasseSet4	= EnumSet.of(Warlock, ElementalSummoner, PhantomSummoner);
	private static final Set<PlayerClass>						subclasseSet5	= EnumSet.of(Sorceror, Spellsinger, Spellhowler);

	private static final EnumMap<PlayerClass, Set<PlayerClass>>	subclassSetMap	= new EnumMap<PlayerClass, Set<PlayerClass>>(PlayerClass.class);

	static
	{
		Set<PlayerClass> subclasses = getSet(null, Third);
		subclasses.removeAll(neverSubclassed);

		mainSubclassSet = subclasses;

		subclassSetMap.put(DarkAvenger, subclasseSet1);
		subclassSetMap.put(Paladin, subclasseSet1);
		subclassSetMap.put(TempleKnight, subclasseSet1);
		subclassSetMap.put(ShillienKnight, subclasseSet1);

		subclassSetMap.put(TreasureHunter, subclasseSet2);
		subclassSetMap.put(AbyssWalker, subclasseSet2);
		subclassSetMap.put(Plainswalker, subclasseSet2);

		subclassSetMap.put(Hawkeye, subclasseSet3);
		subclassSetMap.put(SilverRanger, subclasseSet3);
		subclassSetMap.put(PhantomRanger, subclasseSet3);

		subclassSetMap.put(Warlock, subclasseSet4);
		subclassSetMap.put(ElementalSummoner, subclasseSet4);
		subclassSetMap.put(PhantomSummoner, subclasseSet4);

		subclassSetMap.put(Sorceror, subclasseSet5);
		subclassSetMap.put(Spellsinger, subclasseSet5);
		subclassSetMap.put(Spellhowler, subclasseSet5);
	}

	PlayerClass(Race pRace, ClassType pType, ClassLevel pLevel)
	{
		_race = pRace;
		_level = pLevel;
		_type = pType;
	}

	public final Set<PlayerClass> getAvailableSubclasses(L2PcInstance player)
	{
		Set<PlayerClass> subclasses = null;

		if (_level != Third)
			return null;

			subclasses = EnumSet.copyOf(mainSubclassSet);

			// Already done in mainSubclassSet
			//subclasses.removeAll(neverSubclassed);

			subclasses.remove(this);

			switch (player.getRace())
			{
			case Elf:
				subclasses.removeAll(getSet(Darkelf, Third));
				break;
			case Darkelf:
				subclasses.removeAll(getSet(Elf, Third));
				break;
			}


			Set<PlayerClass> unavailableClasses = subclassSetMap.get(this);

			if (unavailableClasses != null)
			{
				subclasses.removeAll(unavailableClasses);
			}

		return subclasses;
	}

	public static final EnumSet<PlayerClass> getSet(Race race, ClassLevel level)
	{
		EnumSet<PlayerClass> allOf = EnumSet.noneOf(PlayerClass.class);

		for (PlayerClass playerClass : EnumSet.allOf(PlayerClass.class))
		{
			if (race == null || playerClass.isOfRace(race))
			{
				if (level == null || playerClass.isOfLevel(level))
				{
					allOf.add(playerClass);
				}
			}
		}

		return allOf;
	}

	public final boolean isOfRace(Race pRace)
	{
		return _race == pRace;
	}

	public final boolean isOfType(ClassType pType)
	{
		return _type == pType;
	}

	public final boolean isOfLevel(ClassLevel pLevel)
	{
		return _level == pLevel;
	}

	public final ClassLevel getLevel()
	{
		return _level;
	}
}
