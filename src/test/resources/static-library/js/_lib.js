/* _lib.js */
var $$ = {
    _inProgress : {
        _count : 0,
        Add : function() {
            $$._inProgress._count++;
        },
        Complete : function() {
            setTimeout(function() {
                $$._inProgress._count--;
                if ($$._inProgress._count < 0) {
                    $$._inProgress._count = 0;
                }
            }, 1);
        },
        IsRunning : function() {
            return $$._inProgress._count > 0;
        }
    },
    ForEach : function(object, func) {
        if (object == null || typeof(func) !== "function") {
            return;
        }
        
        for (var key in object) {
            if (object.hasOwnProperty(key)) {
                var  result = func(object[key], key);
                if (result === true) {
                    break;
                }
            }
        }
    },
    Init : {
        _functions : [],
        Add : function(func, index) {
            if (typeof(func) !== "function") {
                throw "Function argument must be a function.";
            }
            
            if (typeof(index) === "number") {
                if ($$.Init._functions[index] != null) {
                    throw "Index " + index + " is already in use";
                }
                $$.Init._functions[index] = func;
            } else {
                var next = $$.Init._functions.filter(function(f) { return f !== undefined && f != null; }).length;
                next++;
                $$.Init._functions[next] = func;
            }
        },
        Run : function() {
            var all = $$.Init._functions.filter(function(f) { return f !== undefined && f != null; });
            for (var i = 0; i < all.length; i++) {
                all[i]();
            }
        }
    }
};
