var dryValidatorModuleName;
(function(moduleName) {
Module(moduleName, function(m) {
	m.format = function(format) {
		var args = arguments;
		return format.replace(/\{(\d+)\}/g, function(m,c) { return args[parseInt(c)+1]});
	};
	
	Class("CharacterClass");
	Class("CharacterClass", {
		has: {
			name:  { is: "rw" },
			label: { is: "rw" },
			regex: { is: "rw" },
			instances: { is: "ro", persistent: true}
		},
		classMethods: {
			register: function(name, label, regex){
				if(!this.instances)
					this.instances = {};
				this.instances[name] =
					new m.CharacterClass({name:name, label:label, regex:regex});
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
	m.CharacterClass.register("Digit", "数字", "[0-9]");
	m.CharacterClass.register("Alnum", "英数字", "[A-Za-z0-9]");
	m.CharacterClass.register("Punct", "記号", '[!\"#\$%&\'\(\)\*\+,\-\.\/:;<=>\?@\[\\\]\^_\`\{\|\}\~]');

	Class("Validator", {
		has: {
			label: { is: "rw" },
			messageFormat: { is:"rw" }
		},
		methods: {
			validate: Joose.emptyFunction
		}
	});
	Class("RequiredValidator", {
		isa: m.Validator,
		has: {
			required: { is: "rw", init: true }
		},
		after: {
			initialize: function() { this.messageFormat = "{0}は必須です"; }
		},
		methods: {
			setup: function(value){
				this.required = Boolean.valueOf(value);
			},
			validate: function(value) {
				if(!value) {
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
	Class("CompositeValidator", {
		isa: m.Validator,
		has: {
			validators: { is: "rw", init: [] }
		},
		classMethods: {
			make: function(obj) {
				var self = this.meta.instantiate();
				self.label = obj['label'] || '項目';
				delete(obj.label);

				for(var key in obj) {
					var validator;
					Joose.A.each(m.meta._elements, function(element) {
						if(element.meta.getName().match('\.'+ Joose.S.uppercaseFirst(key) +'Validator$')) {
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
				var results = [];
				Joose.A.each(this.validators, function(validator) {
					var result = validator.validate(value)
					if (result)
						Joose.A.each(result, function(msg) { results.push(msg) });
				});
				return results;
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
})(dryValidatorModuleName || "DryValidator");
