package ru.catssoftware.gameserver.network.serverpackets;

public class UseSharedGroupItem extends L2GameServerPacket
{
  private int _itemId;
  private int _grpId;
  private int _remainedTime;
  private int _totalTime;

  public UseSharedGroupItem(int itemId, int grpId, int remainedTime, int totalTime)
  {
    this._itemId = itemId;
    this._grpId = grpId;
    this._remainedTime = (remainedTime/1000);
    this._totalTime = (totalTime/1000);
  }

  protected void writeImpl()
  {
    writeC(254);
    writeH(73);

    writeD(this._itemId);
    writeD(this._grpId);
    writeD(this._remainedTime);
    writeD(this._totalTime);
  }

  public String getType()
  {
    return "[S] FE:49 ExUseSharedGroupItem";
  }
}