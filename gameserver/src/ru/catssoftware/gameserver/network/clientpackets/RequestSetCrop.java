package ru.catssoftware.gameserver.network.clientpackets;

import ru.catssoftware.Config;
import ru.catssoftware.gameserver.instancemanager.CastleManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager;
import ru.catssoftware.gameserver.instancemanager.CastleManorManager.CropProcure;
import javolution.util.FastList;

public class RequestSetCrop extends L2GameClientPacket
{
	private static final String	_C__D0_0B_REQUESTSETCROP	= "[C] D0:0B RequestSetCrop";

	private int					_size, _manorId;
	private int[]				_items;

	@Override
	protected void readImpl()
	{
		_manorId = readD();
		_size = readD();
			if (_size * 13 > getByteBuffer().remaining() || _size > 500)
			{
				_size = 0;
				return;
			}
		_items = new int[_size * 4];
		for (int i = 0; i < _size; i++)
		{
			int itemId = readD();
			_items[(i * 4)] = itemId;
			int sales = readD();
			_items[i * 4 + 1] = sales;
			int price = readD();
			_items[i * 4 + 2] = price;
			int type = readC();
			_items[i * 4 + 3] = type;
		}
	}

	@Override
	protected void runImpl()
	{
		if (_size < 1)
			return;

		FastList<CropProcure> crops = new FastList<CropProcure>();
		for (int i = 0; i < _size; i++)
		{
			int id = _items[(i * 4)];
			int sales = _items[i * 4 + 1];
			int price = _items[i * 4 + 2];
			int type = _items[i * 4 + 3];
			if (id > 0)
			{
				CropProcure s = CastleManorManager.getInstance().getNewCropProcure(id, sales, type, price, sales);
				crops.add(s);
			}
		}

		CastleManager.getInstance().getCastleById(_manorId).setCropProcure(crops, CastleManorManager.PERIOD_NEXT);
		if (Config.ALT_MANOR_SAVE_ALL_ACTIONS)
			CastleManager.getInstance().getCastleById(_manorId).saveCropData(CastleManorManager.PERIOD_NEXT);
	}

	@Override
	public String getType()
	{
		return _C__D0_0B_REQUESTSETCROP;
	}
}