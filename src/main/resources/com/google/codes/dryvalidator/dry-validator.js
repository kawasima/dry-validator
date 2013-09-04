var dryValidatorModuleName;
(function(moduleName) {
    var root = this;
    var DV;
    if (typeof exports !== 'undefined') {
        DV = exports;
    } else {
        DV = root[moduleName] = {};
    }

	DV.format = function(format) {
		var args = arguments;
		return format
			.replace(/\{(\d+)\}/g, function(m,c) { return args[parseInt(c)+1]})
			.replace("{count}", (this.getCount) ? this.getCount() : "");
	};

	Form = DV.Form = function() {

	};

	_.extend(Form.prototype, {}, {
        _isFormItem: function (el) {
            if (!el || !el.name || !el.getAttribute("name")) return false;
            return (el.tagName == "input" || el.tagName == "INPUT")
                    || (el.tagName == "textarea" || el.tagName == "TEXTAREA")
                    || (el.tagName == "select" || el.tagName == "SELECT");
        },
        _putValue: function (el, value, multiple) {
            var ctx = this.formItems;
            var names = el.getAttribute("name").split("\.");
            var propName = names.pop();
            _.each(names, function (n) {
                // for Nested properties (e.g. foo[0].bar[1].name)
                if (n.match(/^(.+)\[(\d+)\]$/)) {
                    n = RegExp.$1;
                    var idx = RegExp.$2;
                    if (!ctx[n])
                        ctx[n] = new Array();
                    for (var i=ctx[n].length; i <= idx; i++)
                        ctx[n].push({});
                    ctx = ctx[n][idx];
                } else {
                    if (!ctx[n])
                        ctx[n] = {};
                    ctx = ctx[n];
                }
            });
            if (typeof(ctx[propName]) == 'undefined') {
                ctx[propName] = undefined;
            }
            if (typeof(value) != 'undefined') {
                if (multiple) {
                    if (!(ctx[propName] instanceof Array))
                        ctx[propName] = (ctx[propName]) ? [ctx[propName]] : [];
                    ctx[propName].push(value);
                } else {
                    ctx[propName] = value;
                }
            }
        },
        _getValue: function (node, multiple) {
            if (node.getAttribute("type") == "radio" || node.getAttribute("type") == "checkbox") {
                if (node.checked) {
                    var val = node.value;
                    return val == "true" ? true : val;
                }
            } else if (node.name == "select" || node.name == "SELECT"){
                var name = node.getAttribute("name");
                var options = node.getElementsByTagName("option");
                var values = [];
                _.each(options, function(option) {
                    if (option.selected) {
                        values.push(option.value);
                    }
                });
                return (multiple) ? values : values.pop();
            } else {
                return node.value;
            }
        },
        _isMultiple: function(el, exists) {
            return ((el.tagName == "select" || el.tagName == "SELECT") && el.getAttribute("multiple"))
                    || ((el.getAttribute("type") == "checkbox" || el.getAttribute("type") == "CHECKBOX") && exists)
        },
        setup: function(forms) {
            var self = this;
            this.formItems = {};
            if (!(forms instanceof Array))
                forms = [forms];
            _.each(forms, function (f) {
                var form = typeof(f.nodeType) == 'number' ? f : document.getElementById(f);
                var exists = {};
                _.each(form.getElementsByTagName("*"), function(item) {
                    if (self._isFormItem(item)){
                        var name = item.getAttribute("name")
                        var multiple = self._isMultiple(item, exists[name]);
                        self._putValue(item, self._getValue(item, multiple), multiple);
                        exists[name] = 1;
                    }
                });
            });

            return this;
        }
    });

    var CharacterClass = DV.CharacterClass = function(options) {
        this.name  = options['name'];
        this.label = options['label'];
        this.regex = options['regex'];
    };

    CharacterClass.get = function(name) {
        var cc = this.instances[name];
        if(!cc) throw "Can't find " + name;
        return cc;
    };
    CharacterClass.register = function(name, label, regex){
        if(!this.instances)
            this.instances = {};

        if(name.indexOf("+") > 0) {
            // 合成文字クラス
            var names = name.split(/\+/);
            var regexes = [];
            _.each(names, function(name) {
                var cc = CharacterClass.get(name);
                regexes.push(cc.regex);
            });
            this.instances[name] =
                new CharacterClass({name:name, label:label,
                    regex: '('+regexes.join('|')+')'});
        } else {
            // 通常の文字クラス
            this.instances[name] =
                new CharacterClass({name:name, label:label, regex:regex});
        }
    };

    _.extend(CharacterClass.prototype, {} , {
        match: function(value) {
            var re = new RegExp("^"+ this.regex +"*$");
            return value.match(re);
        }
	});

	CharacterClass.register("Katakana", "カタカナ", "[ァ-ヶー]");
	CharacterClass.register("Hiragana", "ひらがな", "[あ-んー]");
	CharacterClass.register("Zenkaku", "全角文字", "[^\u0000-\u007f]")
	CharacterClass.register("Lower", "小文字の英字", "[a-z]");

	CharacterClass.register("Upper", "大文字の英字", "[A-Z]");
	CharacterClass.register("Alpha", "アルファベット", "[A-Za-z]");
	CharacterClass.register("Digit", "半角数字", "[0-9]");
	CharacterClass.register("Alnum", "半角英数字", "[A-Za-z0-9]");
	CharacterClass.register("Punct", "記号", '[!"#\$%&\'\\(\\)\\*\\+,\\-\\.\\/:;<=>\\?@\\[\\\\\\]\\^_\\`\\{\\|\\}\\~]');
	CharacterClass.register("Alnum+Punct", "半角英数記号");

    var Validator = DV.Validator = {
        validate: function() {},
        getValue: function(id) {
            var self = this;
            var ctx = this.context;
            _.each(id.split("\."), function (n, i) {
                if (n.match(/^(.+)\[(\d+)?\]$/)) {
                    n = RegExp.$1;
                    var idx = RegExp.$2;
                    if (idx == "" || typeof(idx) == 'undefined')
                        idx = self.counts[i]-1;
                    ctx = ctx[n][idx];
                } else {
                    ctx = ctx[n];
                }
            });
            return ctx;
        },
        getCount: function(depth) {
            if (typeof(depth) === 'undefined')
                depth = 0;
            if (!this.counts || this.counts.length == 0) {
                return 0;
            } else {
                return this.counts[this.counts.length - 1 + depth];
            }
        }
    };

    var Executor = DV.Executor = function() {
        this.validators = [];
    };
	_.extend(Executor.prototype, {}, {
        addValidator: function (id, validator) {
            this.validators[id] = validator;
        },
        _findValidator: function (id) {
            var validator = this.validators[id];
            if (!validator) {
                _.chain(this.validators).pairs().each(function(pairs) {
                    var re = new RegExp("^"+pairs[0]+"$");
                    if (re.exec(id))
                        validator = pairs[1];
                });
            }
            return validator;
        },
        _execute: function(value, id, counts) {
            if (!counts)
                counts = [];

            if (value instanceof Array) {
                for (var i=0; i<value.length; i++) {
                    counts.push(i+1);
                    this._execute(value[i], id + "[]", counts);
                    counts.pop();
                }
            } else if (value != null && typeof(value) == 'object') {
                var self = this;
                _.chain(value).pairs().each(function (pair) {
                    self._execute(pair[1], id + "." + pair[0], counts);
                });
            } else {
                var validator = this._findValidator(id);

                if (validator) {
                    validator.context = this._form;
                    validator.counts = counts;
                    var msgs = validator.validate(value);
                    if (msgs) {
                        var indexes = counts.slice(0);
                        id = id.replace(/\[\]/g, function() { return "[" + (indexes.shift()-1) + "]" });
                        this._messages[id] = this._messages[id] ? this._messages[id].concat(msgs) : msgs;
                    }
                }
            }
        },
        execute: function (form) {
            var self = this;
            this._messages = {};
            this._form = form;
            _.chain(this._form).pairs().each(function(pair) {
                self._execute(pair[1], pair[0]);
            });
            return this._messages;
        }
    });

    var RequiredValidator = DV.RequiredValidator = function(options) {
        if (options["label"]) this.label = options["label"];
        this.messageFormat = "{0}は必須です。";
    };

	_.extend(RequiredValidator.prototype, Validator, {
        setup: function(value){
            this.required = (value == true || value == "true");
        },
        validate: function(value) {
            if(this.required && (value == null || String(value) == "" || typeof(value) == "undefined")) {
                return [DV.format.apply(this, [this.messageFormat, this.label])];
            }
        }
    });

    var MaxLengthValidator = DV.MaxLengthValidator = function(options) {
        if (options["label"]) this.label = options["label"];
        this.messageFormat = "{0}は{1}文字以内で入力してください。";
    };

    _.extend(MaxLengthValidator.prototype, Validator, {
        setup: function(value) {
            this.length = Number(value);
        },
        validate: function(value) {
            if(value && value.toString().length > this.length) {
                return [DV.format.apply(this, [this.messageFormat, this.label, this.length])];
            }
        }
    });

	var LetterTypeValidator = DV.LetterTypeValidator = function(options) {
        if (options["label"]) this.label = options["label"];
	    this.messageFormat = "{0}は{1}で入力してください。";
	};

	_.extend(LetterTypeValidator.prototype, Validator, {
        setup: function(value) {
            this.characterClass = CharacterClass.get(value);
        },
        validate: function(value) {
            if(value && !this.characterClass.match(value.toString())) {
                return [DV.format.apply(this, [this.messageFormat, this.label, this.characterClass.label])];
            }
        }
	});

    var RangeValidator = DV.RangeValidator = function(options) {
        if (options["label"]) this.label = options["label"];
        this.min = 0;
        this.max = Number.MAX_VALUE;
        this.messageFormat = null;
        this.wittyMessageFormat = {
            min:    "{0}は{1}以上の値でなくてはなりません。",
            max:    "{0}は{1}以下の値でなくてはなりません。"
        };
    };

    _.extend(RangeValidator.prototype, Validator, {
        setup: function(value) {
            var obj = eval("("+value+")");
            if(obj["min"])
                this.min = obj["min"];
            if(obj["max"])
                this.max = obj["max"];
        },
        validate: function(value) {
            if(!value)
                return;
            if(value < this.min || value > this.max) {
                if(this.messageFormat)
                    return [DV.format.apply(this, [this.messageFormat, this.label])];
                else if (value < this.min)
                    return [DV.format.apply(this, [this.wittyMessageFormat["min"], this.label, this.min])];
                else if (value > this.max)
                    return [DV.format.apply(this, [this.wittyMessageFormat["max"], this.label, this.max])];
            }
        }

    });

    var SelectionValidator = DV.SelectionValidator = function(options) {
        if (options["label"]) this.label = options["label"];
        this.min = 0;
        this.max = Number.MAX_VALUE;
        this.messageFormat = null;
        this.wittyMessageFormat = {
            min:    "{0}は{1}個以上選択してください。",
            max:    "{0}は{1}個までしか選択できません。",
            minmax: "{0}は{1}個以上{2}個以下で選択してください。",
            min1:   "{0}を選択してください。"
        };
    };

	_.extend(SelectionValidator.prototype, Validator, {
        setup: function(value) {
            var obj = eval("("+value+")");
            if(obj["min"])
                this.min = obj["min"];
            if(obj["max"])
                this.max = obj["max"];
        },
        validate: function(value) {
            // Valueがundefined,null,空文字の場合は空配列にする
            // すなわち最小選択数が1以上の場合は空文字でも未選択になるということ
            if (!(value instanceof Array))
                value = [value];

            var valueLen = 0;
            if (value.length > 1 || !(value[0]==null || String(value[0]) == "" || typeof(value[0]) == "undefined")) {
                valueLen = value.length;
            }
            if(valueLen < this.min || valueLen > this.max) {
                if(this.messageFormat)
                    return [DV.format.apply(this, [this.messageFormat, this.label])];

                if (this.min > 0 && this.max < Number.MAX_VALUE)
                    return [DV.format.apply(this, [this.wittyMessageFormat["minmax"], this.label, this.min, this.max])];
                else if (this.min == 1)
                    return [DV.format.apply(this, [this.wittyMessageFormat["min1"], this.label])];
                else if (this.min > 0)
                    return [DV.format.apply(this, [this.wittyMessageFormat["min"], this.label, this.min])];
                else if (this.max < Number.MAX_VALUE)
                    return [DV.format.apply(this, [this.wittyMessageFormat["max"], this.label, this.max])];
            }
        }
	});

	var FunctionValidator = DV.FunctionValidator = function(options) {
        if (options["label"]) this.label = options["label"];
	};

	_.extend(FunctionValidator.prototype, Validator, {
        setup: function(value) {
            if(value instanceof Function) {
                this.func = value;
            } else {
                eval('this.func = function(value){'+ value + '}');
            }
            if(!this.func instanceof Function)
                throw "value must be Function.";
        },
        validate: function(value) {
            return this.func.apply(this, [value]);
        }
	});

	var CompositeValidator = DV.CompositeValidator = function() {
        this.validators = [];
        this.messageDecorator;
	};

    CompositeValidator.make = function(obj) {
        var self = new CompositeValidator();
        self.label = obj['label'] || 'item';

        if (_.isString(obj['messageDecorator'])) {
            self.messageDecorator = eval("function (message) {" + obj['messageDecorator'] + "}");
        } else if (_.isFunction(obj['messageDecorator'])) {
            self.messageDecorator = obj['messageDecorator'];
        }
        _.chain(obj).keys().without('label', 'messageDecorator').each(function(key) {
            var validatorClass = DV[key.charAt(0).toUpperCase()
                + key.substring(1)
                + "Validator"];
            if (validatorClass) {
                var validator = new validatorClass({ label: self.label });
                validator.setup(obj[key]);
                self.validators.push(validator);
            }
        });
        return self;
    };

	_.extend(CompositeValidator.prototype, Validator, {
        validate: function(value) {
            var self = this;
            var results = [];
            _.each(this.validators, function(validator) {
                validator.counts =  self.counts;
                validator.context = self.context;
                var result = validator.validate(value);
                if (result) {
                    _.each(result, function(msg) {
                        if (self.messageDecorator)
                            msg = self.messageDecorator(msg);
                        results.push(msg);
                    });
                }
            });
            return results;
        },
        stringify: function() {
            return _.reduce(this.validators, function(res, validator) {
                res += validator.toString() + ",";
            }, "");
        }
    });

	// Copyright 2005-2007 Kawasaki Yusuke <u-suke@kawa.net>
	var DOM = DV.DOM = {
        parseElement: function(elem) {
            if(elem.nodeType == 7) return;

            if(elem.nodeType == 3 || elem.nodeType == 4) {
                var bool = elem.nodeValue.match(/[^\x00-\x20]/);
                if(bool == null) return;
                return elem.nodeValue;
            }

            var retval;
            var cnt = {};

            if(elem.attributes && elem.attributes.length) {
                retval = {};
                for (var i=0; i<elem.attributes.length; i++) {
                    var key = elem.attributes[i].nodeName;
                    if(typeof(key) != "string") continue;
                    var val = elem.attributes[i].nodeValue;
                    if(!val) continue;
                    if(typeof(cnt[key]) == "undefined") cnt[key] = 0;
                    cnt[key]++;
                    this.addNode(retval, key, cnt[key], val);
                }
            }

            if(elem.childNodes && elem.childNodes.length) {
                var textonly = true;
                if (retval) textonly = false;
                for (var i=0; i<elem.childNodes.length && textonly; i++) {
                    var ntype = elem.childNodes[i].nodeType;
                    if (ntype == 3 || ntype == 4) continue;
                    textonly = false;
                }
                if (textonly) {
                    if (!retval) retval = "";
                    for (var i=0; i<elem.childNodes.length; i++) {
                        retval += elem.childNodes[i].nodeValue;
                    }
                } else {
                    if (!retval) retval = {};
                    for(var i=0; i<elem.childNodes.length; i++) {
                        var key = elem.childNodes[i].nodeName;
                        if (typeof(key) != "string") continue;
                        var val = this.parseElement(elem.childNodes[i]);
                        if (!val) continue;
                        if (typeof(cnt[key]) == "undefined") cnt[key] = 0;
                        cnt[key]++;
                        this.addNode(retval, key, cnt[key], val);
                    }
                }
            }
            return retval;
        },
        addNode: function (hash, key, cnts, val){
            if (this.usearray == true) {
                if (cnts == 1) hash[key] = [];
                hash[key][hash[key].length] = val;
            } else if (this.usearray == false) {
                if (cnts == 1) hash[key] = val;
            } else if (this.usearray == null) {
                if (cnts == 1) {
                    hash[key] = val;
                } else if (cnts == 2) {
                    hash[key] = [ hash[key], val];
                } else {
                    hash[key][hash[key].length] = val;
                }
            } else if (this.usearray[key]) {
                if (cnts == 1) hash[key] = [];

                hash[key][hash[key].length] = val;
            } else {
                if (cnts == 1) hash[key] = val;
            }
        }
	};
})(dryValidatorModuleName || "DRYValidator");
