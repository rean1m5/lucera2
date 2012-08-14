package ru.catssoftware.gameserver.model.base;

public enum ClassId
{
	fighter(0x00, false, Race.Human, null),

	warrior(0x01, false, Race.Human, fighter),
	gladiator(0x02, false, Race.Human, warrior),
	warlord(0x03, false, Race.Human, warrior),
	knight(0x04, false, Race.Human, fighter),
	paladin(0x05, false, Race.Human, knight),
	darkAvenger(0x06, false, Race.Human, knight),
	rogue(0x07, false, Race.Human, fighter),
	treasureHunter(0x08, false, Race.Human, rogue),
	hawkeye(0x09, false, Race.Human, rogue),

	mage(0x0a, true, Race.Human, null),
	wizard(0x0b, true, Race.Human, mage),
	sorceror(0x0c, true, Race.Human, wizard),
	necromancer(0x0d, true, Race.Human, wizard),
	warlock(0x0e, true, Race.Human, wizard, true),
	cleric(0x0f, true, Race.Human, mage),
	bishop(0x10, true, Race.Human, cleric),
	prophet(0x11, true, Race.Human, cleric),

	elvenFighter(0x12, false, Race.Elf, null),
	elvenKnight(0x13, false, Race.Elf, elvenFighter),
	templeKnight(0x14, false, Race.Elf, elvenKnight),
	swordSinger(0x15, false, Race.Elf, elvenKnight),
	elvenScout(0x16, false, Race.Elf, elvenFighter),
	plainsWalker(0x17, false, Race.Elf, elvenScout),
	silverRanger(0x18, false, Race.Elf, elvenScout),

	elvenMage(0x19, true, Race.Elf, null),
	elvenWizard(0x1a, true, Race.Elf, elvenMage),
	spellsinger(0x1b, true, Race.Elf, elvenWizard),
	elementalSummoner(0x1c, true, Race.Elf, elvenWizard, true),
	oracle(0x1d, true, Race.Elf, elvenMage),
	elder(0x1e, true, Race.Elf, oracle),

	darkFighter(0x1f, false, Race.Darkelf, null),
	palusKnight(0x20, false, Race.Darkelf, darkFighter),
	shillienKnight(0x21, false, Race.Darkelf, palusKnight),
	bladedancer(0x22, false, Race.Darkelf, palusKnight),
	assassin(0x23, false, Race.Darkelf, darkFighter),
	abyssWalker(0x24, false, Race.Darkelf, assassin),
	phantomRanger(0x25, false, Race.Darkelf, assassin),

	darkMage(0x26, true, Race.Darkelf, null),
	darkWizard(0x27, true, Race.Darkelf, darkMage),
	spellhowler(0x28, true, Race.Darkelf, darkWizard),
	phantomSummoner(0x29, true, Race.Darkelf, darkWizard, true),
	shillienOracle(0x2a, true, Race.Darkelf, darkMage),
	shillienElder(0x2b, true, Race.Darkelf, shillienOracle),

	orcFighter(0x2c, false, Race.Orc, null),
	orcRaider(0x2d, false, Race.Orc, orcFighter),
	destroyer(0x2e, false, Race.Orc, orcRaider),
	orcMonk(0x2f, false, Race.Orc, orcFighter),
	tyrant(0x30, false, Race.Orc, orcMonk),

	orcMage(0x31, false, Race.Orc, null),
	orcShaman(0x32, false, Race.Orc, orcMage),
	overlord(0x33, true, Race.Orc, orcShaman),
	warcryer(0x34, true, Race.Orc, orcShaman),

	dwarvenFighter(0x35, false, Race.Dwarf, null),
	scavenger(0x36, false, Race.Dwarf, dwarvenFighter),
	bountyHunter(0x37, false, Race.Dwarf, scavenger),
	artisan(0x38, false, Race.Dwarf, dwarvenFighter),
	warsmith(0x39, false, Race.Dwarf, artisan),

	dummyEntry1(58, false, null, null),
	dummyEntry2(59, false, null, null),
	dummyEntry3(60, false, null, null),
	dummyEntry4(61, false, null, null),
	dummyEntry5(62, false, null, null),
	dummyEntry6(63, false, null, null),
	dummyEntry7(64, false, null, null),
	dummyEntry8(65, false, null, null),
	dummyEntry9(66, false, null, null),
	dummyEntry10(67, false, null, null),
	dummyEntry11(68, false, null, null),
	dummyEntry12(69, false, null, null),
	dummyEntry13(70, false, null, null),
	dummyEntry14(71, false, null, null),
	dummyEntry15(72, false, null, null),
	dummyEntry16(73, false, null, null),
	dummyEntry17(74, false, null, null),
	dummyEntry18(75, false, null, null),
	dummyEntry19(76, false, null, null),
	dummyEntry20(77, false, null, null),
	dummyEntry21(78, false, null, null),
	dummyEntry22(79, false, null, null),
	dummyEntry23(80, false, null, null),
	dummyEntry24(81, false, null, null),
	dummyEntry25(82, false, null, null),
	dummyEntry26(83, false, null, null),
	dummyEntry27(84, false, null, null),
	dummyEntry28(85, false, null, null),
	dummyEntry29(86, false, null, null),
	dummyEntry30(87, false, null, null),

	duelist(0x58, false, Race.Human, gladiator),
	dreadnought(0x59, false, Race.Human, warlord),
	phoenixKnight(0x5a, false, Race.Human, paladin),
	hellKnight(0x5b, false, Race.Human, darkAvenger),
	sagittarius(0x5c, false, Race.Human, hawkeye),
	adventurer(0x5d, false, Race.Human, treasureHunter),
	archmage(0x5e, true, Race.Human, sorceror),
	soultaker(0x5f, true, Race.Human, necromancer),
	arcanaLord(0x60, true, Race.Human, warlock, true),
	cardinal(0x61, true, Race.Human, bishop),
	hierophant(0x62, true, Race.Human, prophet),

	evaTemplar(0x63, false, Race.Elf, templeKnight),
	swordMuse(0x64, false, Race.Elf, swordSinger),
	windRider(0x65, false, Race.Elf, plainsWalker),
	moonlightSentinel(0x66, false, Race.Elf, silverRanger),
	mysticMuse(0x67, true, Race.Elf, spellsinger),
	elementalMaster(0x68, true, Race.Elf, elementalSummoner, true),
	evaSaint(0x69, true, Race.Elf, elder),

	shillienTemplar(0x6a, false, Race.Darkelf, shillienKnight),
	spectralDancer(0x6b, false, Race.Darkelf, bladedancer),
	ghostHunter(0x6c, false, Race.Darkelf, abyssWalker),
	ghostSentinel(0x6d, false, Race.Darkelf, phantomRanger),
	stormScreamer(0x6e, true, Race.Darkelf, spellhowler),
	spectralMaster(0x6f, true, Race.Darkelf, phantomSummoner, true),
	shillienSaint(0x70, true, Race.Darkelf, shillienElder),

	titan(0x71, false, Race.Orc, destroyer),
	grandKhauatari(0x72, false, Race.Orc, tyrant),
	dominator(0x73, true, Race.Orc, overlord),
	doomcryer(0x74, true, Race.Orc, warcryer),

	fortuneSeeker(0x75, false, Race.Dwarf, bountyHunter),
	maestro(0x76, false, Race.Dwarf, warsmith),

	dummyEntry31(119, false, null, null),
	dummyEntry32(120, false, null, null),
	dummyEntry33(121, false, null, null),
	dummyEntry34(122, false, null, null);


	private final int		_id;
	private final boolean	_isMage;
	private final boolean	_isSummoner;
	private final Race		_race;
	private final ClassId	_parent;

	private ClassId(int id, boolean isMage, Race race, ClassId parent)
	{
		_id = id;
		_isMage = isMage;
		_race = race;
		_parent = parent;
		_isSummoner = false;
	}

	private ClassId(int id, boolean isMage, Race race, ClassId parent, boolean sumonner)
	{
		_id = id;
		_isMage = isMage;
		_race = race;
		_parent = parent;
		_isSummoner = sumonner;
	}

	public final int getId()
	{
		return _id;
	}

	public final boolean isMage()
	{
		return _isMage;
	}

	public final boolean isSummoner()
	{
		return _isSummoner;
	}

	public final Race getRace()
	{
		return _race;
	}

	public final boolean childOf(ClassId cid)
	{
		return _parent != null && (_parent == cid || _parent.childOf(cid));

	}

	public final boolean equalsOrChildOf(ClassId cid)
	{
		return this == cid || childOf(cid);
	}

	public final int level()
	{
		if (_parent == null)
			return 0;

		return 1 + _parent.level();
	}

	public final ClassId getParent()
	{
		return _parent;
	}

	public ClassId[] getAllTree()
	{
		return getAllTree(new ClassId[level() + 1]);
	}

	public ClassId[] getAllTree(ClassId[] classes)
	{
		for(ClassId classId : values())
			if (_parent == null || _parent.equals(classId))
			{
				classes[classId.level()] = classId;
				if (classId.level() > 0)
				{
					return classId.getAllTree(classes);
				}
				break;
			}
		return classes;
	}
}
