package org.dynjs.runtime.builtins.types.date.prototype;

import org.dynjs.exception.ThrowException;
import org.dynjs.runtime.ExecutionContext;
import org.dynjs.runtime.GlobalObject;
import org.dynjs.runtime.builtins.types.date.AbstractDateFunction;
import org.dynjs.runtime.builtins.types.date.DynDate;

public class GetSeconds extends AbstractDateFunction {

    public GetSeconds(GlobalObject globalObject) {
        super(globalObject);
    }

    @Override
    public Object call(ExecutionContext context, Object self, Object... args) {
        if (!(self instanceof DynDate)) {
            throw new ThrowException(context, context.createTypeError("getMinutes() may only be used with Dates"));
        }

        DynDate date = (DynDate) self;

        if (date.isNaN()) {
            return Double.NaN;
        }

        long t = date.getTimeValue();
        return (long) secFromTime(localTime(context, t));
    }
    
    @Override
    public void setFileName() {
        this.filename = "org/dynjs/runtime/builtins/types/date/prototype/GetSeconds.java";
    }

    @Override
    public void setupDebugContext() {
        this.debugContext = "<native function: getSeconds>";
    }
}
