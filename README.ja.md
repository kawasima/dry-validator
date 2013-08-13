DRY validator
=============

DRY validator はWebアプリケーションのバリデーションをDRYの原則にしたがって、サーバサイドとクライアントサイドで
1箇所にまとめるためのライブラリです。

バリデーションのロジックはJavascriptで書き、クライアントサイドではブラウザのJavascriptエンジンで、
サーバサイドではRhinoで動作します。

バリデーションの定義は以下のように宣言的に書けます。
型の定義はプリセットがあり、それを直ぐに使えますが自分で定義したものを使うことも可能です。
Javascriptのスクリプト言語的特性を生かして、入力チェック内容をここに定義することもできます。

また、一覧入力などで何番目の項目がエラーになったかを示す場合など、メッセージを動的にカスタマイズする
ことも可能です。

```javascript
{
  "familyName": {
		"label": "氏名",
		"required": true,
		"maxLength": 10,
		"letterType": "Zenkaku",
		"function": "return ['特別なチェック']"
	},
	"children[].name": {
		"label": "お子様の名前",
		"required": true,
		"maxLength": 5,
		"messageDecorator": "return DRYValidator.format.apply(this, ['{count}人目の' + message])"
	}
}
```
