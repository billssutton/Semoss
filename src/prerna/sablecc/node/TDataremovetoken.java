/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class TDataremovetoken extends Token
{
    public TDataremovetoken()
    {
        super.setText("data.remove");
    }

    public TDataremovetoken(int line, int pos)
    {
        super.setText("data.remove");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TDataremovetoken(getLine(), getPos());
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTDataremovetoken(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TDataremovetoken text.");
    }
}
