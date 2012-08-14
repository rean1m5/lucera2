package ru.catssoftware.gameserver.threadmanager;

public abstract class FIFOExecutableQueue implements Runnable
{
    protected static final byte NONE = 0;
    protected static final byte QUEUED = 1;
    protected static final byte RUNNING = 2;
    
    protected volatile byte _state = NONE;
    
    protected boolean execute()
    {
        synchronized (this)
        {
            if (_state != NONE)
                return false;
            
            _state = QUEUED;
        }
        
        return true;
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
                    removeAndExecuteFirst();
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
    
    protected abstract boolean isEmpty();
    
    protected abstract void removeAndExecuteFirst();
}
