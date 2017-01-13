/* This file was generated by SableCC (http://www.sablecc.org/). */

package prerna.sablecc2.node;

import prerna.sablecc2.analysis.*;

@SuppressWarnings("nls")
public final class TAsOp extends Token
{
    public TAsOp()
    {
        super.setText(".as");
    }

    public TAsOp(int line, int pos)
    {
        super.setText(".as");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TAsOp(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTAsOp(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TAsOp text.");
    }
}
