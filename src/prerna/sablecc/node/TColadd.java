/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc.node;

import prerna.sablecc.analysis.*;

@SuppressWarnings("nls")
public final class TColadd extends Token
{
    public TColadd()
    {
        super.setText("col.add");
    }

    public TColadd(int line, int pos)
    {
        super.setText("col.add");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TColadd(getLine(), getPos());
    }

    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTColadd(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TColadd text.");
    }
}
