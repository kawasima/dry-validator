var dryValidatorModuleName;
(function(moduleName) {
Module(moduleName, function(m) {
	m.format = function(format) {
		var args = arguments;
		return format.replace(/\{(\d+)\}/g, function(m,c) { return args[parseInt(c)+1]});
	};

	var setupFormItems = function(nodeList) {
		var self = this;
		Joose.A.each(nodeList, function(item) {
			var name = item.getAttribute("name");
			if (!self.formItems[name])
				self.formItems[name] = null;

			if (item.getAttribute("type") == "radio") {
				if (item.checked) {
					var val = item.value;
					val = val == "true" ? true : val;
					self.formItems[name] = val;
				}
			} else if (item.getAttribute("type") =="checkbox") {
				if (item.checked) {
					var val = item.value;
					val = val == "true" ? true : val;
					if (!self.formItems[name])
						self.formItems[name] = [];
					self.formItems[name].push(val);
				}
			} else {
				self.formItems[name] = item.value;
			}
		});
	};

	Class("Form", {
		has: {
			formItems: { is: "rw", init: {} }
		},
		after: {
			initialize: function() { m.currentForm = this }
		},
		methods: {
			setup: function(formId) {
				var self = this;
				this.formItems = {};
				var form = document.getElementById(formId);
				setupFormItems.apply(this, [form.getElementsByTagName("input")]);
				setupFormItems.apply(this, [form.getElementsByTagName("textarea")]);

				Joose.A.each(form.getElementsByTagName("select"), function(item) {
					var name = item.getAttribute("name");
					var options = item.getElementsByTagName("option");
					var values = [];
					Joose.A.each(options, function(option) {
						if (option.selected) {
							values.push(option.value);
						}
					});
					self.formItems[name] = (values.length > 0) ? values : null;
				});

				return this;
			}
		}
	});

	Class("CharacterClass");
	Class("CharacterClass", {
		has: {
			name:  { is: "rw" },
			label: { is: "rw" },
			regex: { is: "rw" },
			instances: { is: "ro", persistent: true}
		},
		classMethods: {
			enable: function(names) {
				if(arguments.length == 0) {
					return this.meta.enabled;
				}
				this.meta.enabled = [];
				if(!(names instanceof Array)) {
					names = [ names ];
				}
				for(var i=0; i<names.length; i++) {
					if(this.instances[names[i]]) {
						this.meta.enabled.push(names[i]);
					}
				}
			},
			register: function(name, label, regex){
				if(!this.instances)
					this.instances = {};

				if(name.indexOf("+") > 0) {
					// 合成文字クラス
					var names = name.split(/\+/);
					var regexes = [];
					Joose.A.each(names, function(name) {
						var cc = m.CharacterClass.get(name);
						regexes.push(cc.getRegex());
					});
					this.instances[name] =
						new m.CharacterClass({name:name, label:label,
							regex: '('+regexes.join('|')+')'});
				} else {
					// 通常の文字クラス
					this.instances[name] =
						new m.CharacterClass({name:name, label:label, regex:regex});
				}
			},
			get: function(name) {
				var cc = this.instances[name];
				if(!cc) throw "Can't find " + name;
				return cc;
			}
		},
		methods: {
			match: function(value) {
				var re = new RegExp("^"+ this.regex +"*$");
				return value.match(re);
			}
		}

	});
	m.CharacterClass.register("Katakana", "カタカナ", "[ァ-ヶー]");
	m.CharacterClass.register("Hiragana", "ひらがな", "[あ-んー]");
	m.CharacterClass.register("Zenkaku", "全角文字", "[^\u0000-\u007f]")
	m.CharacterClass.register("Lower", "小文字の英字", "[a-z]");

	m.CharacterClass.register("Upper", "大文字の英字", "[A-Z]");
	m.CharacterClass.register("Alpha", "アルファベット", "[A-Za-z]");
	m.CharacterClass.register("Digit", "半角数字", "[0-9]");
	m.CharacterClass.register("Alnum", "半角英数字", "[A-Za-z0-9]");
	m.CharacterClass.register("Punct", "記号", '[!\"#\$%&\'\(\)\*\+,\-\.\/:;<=>\?@\[\\\]\^_\`\{\|\}\~]');
	m.CharacterClass.register("Alnum+Punct", "半角英数記号");

	Class("Validator", {
		has: {
			label: { is: "rw" },
			messageFormat: { is:"rw" },
			context: { is: "rw" }
		},
		methods: {
			validate: Joose.emptyFunction,
			getValue: function(id) {
				return this.context[id];
			}
		}
	});

	Class("Executor", {
		has: {
			validators: { is: "ro", init: function() { return {} } }
		},
		methods: {
			addValidator: function(id, validator) {
				this.validators[id] = validator;
			},
			execute: function(form) {
				var self = this;
				var messages = {};
				Joose.O.each(form, function(value, id) {
					var validator = self.validators[id];
					if (!validator) {
						Joose.O.each(self.validators, function(v, vid) {
							var re = new RegExp("^"+vid+"$");
							if (re.exec(id))
								validator = v;
						});
					}

					if (validator) {
						validator.setContext(form);
						messages[id] = validator.validate(value);
					}
				});
				return messages;
			}
		}
	});
	Class("RequiredValidator", {
		isa: m.Validator,
		has: {
			required: { is: "rw", init: true }
		},
		after: {
			initialize: function() { this.messageFormat = "{0}は必須です。"; }
		},
		methods: {
			setup: function(value){
				this.required = (value == true || value == "true");
			},
			validate: function(value) {
				if(this.required && (value == null || String(value) == "" || typeof(value) == "undefined")) {
					return [m.format(this.messageFormat, this.label)];
				}
			}
		}
	});
	Class("MaxLengthValidator", {
		isa: m.Validator,
		has: {
			length: { is: "rw" }
		},
		after: {
			initialize: function() { this.messageFormat = "{0}は{1}文字以内で入力してください。"; }
		},
		methods: {
			setup: function(value) {
				this.length = Number(value);
			},
			validate: function(value) {
				if(value && value.toString().length > this.length) {
					return [m.format(this.messageFormat, this.label, this.length)];
				}
			}
		}
	});
	Class("LetterTypeValidator", {
		isa: m.Validator,
		has: {
			characterClass: { is: "rw", init: [] }
		},
		after: {
			initialize: function() { this.messageFormat = "{0}は{1}で入力してください。"; }
		},
		methods: {
			setup: function(value) {
				this.characterClass = m.CharacterClass.get(value);
			},
			validate: function(value) {
				if(value && !this.characterClass.match(value.toString())) {
					return [m.format(this.messageFormat, this.label, this.characterClass.getLabel())];
				}
			}
		}
	});

	Class("RangeValidator", {
		isa: m.Validator,
		has: {
			min: { is:"rw", init: 0 },
			max: { is:"rw", init:Number.MAX_VALUE }
		},
		after: {
			initialize: function() {
				this.messageFormat = null;
				this.wittyMessageFormat = {
					min:    "{0}は{1}以上の値でなくてはなりません。",
					max:    "{0}は{1}以下の値でなくてはなりません。"
				}
			}
		},
		methods: {
			setup: function(value) {
				var obj = eval("("+value+")");
				if(obj["min"])
					this.setMin(obj["min"]);
				if(obj["max"])
					this.setMax(obj["max"]);
			},
			validate: function(value) {
				if(!value)
					return;
				if(value < this.min || value > this.max) {
					if(this.messageFormat)
						return [m.format(this.messageFormat, this.label)];
					else if (value < this.min)
						return [m.format(this.wittyMessageFormat["min"], this.label, this.min)];
					else if (value > this.max)
						return [m.format(this.wittyMessageFormat["max"], this.label, this.max)];
				}
			}
		}
	});

	Class("SelectionValidator", {
		isa: m.Validator,
		has: {
			min: { is:"rw", init: 0 },
			max: { is:"rw", init: Number.MAX_VALUE },
			wittyMessageFormat: { is:"rw" }
		},
		after: {
			initialize: function() {
				this.messageFormat = null;
				this.wittyMessageFormat = {
					min:    "{0}は{1}個以上選択してください。",
					max:    "{0}は{1}個までしか選択できません。",
					minmax: "{0}は{1}個以上{2}個以下で選択してください。",
					min1:   "{0}を選択してください。"
				}
			}
		},
		methods: {
			setup: function(value) {
				var obj = eval("("+value+")");
				if(obj["min"])
					this.setMin(obj["min"]);
				if(obj["max"])
					this.setMax(obj["max"]);
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
						return [m.format(this.messageFormat, this.label)];

					if (this.min > 0 && this.max < Number.MAX_VALUE)
						return [m.format(this.wittyMessageFormat["minmax"], this.label, this.min, this.max)];
					else if (this.min == 1)
						return [m.format(this.wittyMessageFormat["min1"], this.label)];
					else if (this.min > 0)
						return [m.format(this.wittyMessageFormat["min"], this.label, this.min)];
					else if (this.max < Number.MAX_VALUE)
						return [m.format(this.wittyMessageFormat["max"], this.label, this.max)];
				}
			}
		}
	});
	Class("FunctionValidator", {
		isa: m.Validator,
		has: {
			func: { is:"rw" }
		},
		after: {
		},
		methods: {
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
		}
	});

	Class("CompositeValidator", {
		isa: m.Validator,
		has: {
			validators: { is: "rw", init: function() {return [];} }
		},
		classMethods: {
			make: function(obj) {
				var self = new m.CompositeValidator();
				self.label = obj['label'] || '項目';
				delete(obj.label);

				for(var key in obj) {
					var validator = null;
					Joose.A.each(m.meta._elements, function(element) {
						if(element.meta.getName().match('\\.'+ Joose.S.uppercaseFirst(key) +'Validator$')) {
							validator = element.meta.instantiate({label:self.label});
						}
					});
					if(validator) {
						validator.setup(obj[key]);
						self.getValidators().push(validator);
					}
				}
				return self;
			}
		},
		methods: {
			validate: function(value) {
				var self = this;
				var results = [];
				Joose.A.each(this.validators, function(validator) {
					validator.setContext(self.getContext());
					var result = validator.validate(value);
					if (result)
						Joose.A.each(result, function(msg) { results.push(msg) });
				});
				return results;
			},
			stringify: function() {
				var res = "";
				Joose.A.each(this.validators, function(validator) {
					res += validator.toString() + ",";
				});
				return res;
			}
		}
	});

	// Copyright 2005-2007 Kawasaki Yusuke <u-suke@kawa.net>
	Class("DOM", {
		classMethods: {
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
		}
	});
});
})(dryValidatorModuleName || "DRYValidator");
