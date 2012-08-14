package ru.catssoftware.gameserver.threadmanager;

import ru.catssoftware.gameserver.ThreadPoolManager;
import javolution.util.FastList;


public abstract class FIFOPacketRunnableQueue<T extends Runnable> implements Runnable
{
    private static final byte NONE = 0;
    private static final byte QUEUED = 1;
    private static final byte RUNNING = 2;
    
    private final FastList<T> _queue = new FastList<T>(50);
    
    private volatile byte _state = NONE;
    
    public final void execute(T t)
    {
        addLast(t);
        
        synchronized (this)
        {
            if (_state != NONE)
                return;
            
            _state = QUEUED;
        }
        
        ThreadPoolManager.getInstance().executeLSGSPacket(this);
    }
    
    private void addLast(T t)
    {
        synchronized (_queue)
        {
            _queue.addLast(t);
        }
    }
    
    private boolean isEmpty()
    {
        synchronized (_queue)
        {
            return _queue.isEmpty();
        }
    }
    
    private T removeFirst()
    {
        synchronized (_queue)
        {
            return _queue.removeFirst();
        }
    }
    
    public void clear()
    {
        synchronized (_queue)
        {
            _queue.clear();
        }
    }
    
    public final void run()
    {
        while (!isEmpty())
        {
            try
            {
                synchronized (this)
                {
                    if (_state == RUNNING)
                        return;
                    
                    _state = RUNNING;
                }
                
                while (!isEmpty())
                    ExecuteWrapper.execute(removeFirst());
            }
            finally
            {
                synchronized (this)
                {
                    _state = NONE;
                }
            }
        }
    }
}
