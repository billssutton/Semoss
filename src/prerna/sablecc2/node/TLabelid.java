/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class TLabelid extends Token
{
    public TLabelid()
    {
        super.setText("l=");
    }

    public TLabelid(int line, int pos)
    {
        super.setText("l=");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TLabelid(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTLabelid(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TLabelid text.");
    }
}
