<html>
	<head>
		<title>This is a Velocity Template HTML</title>
		<link href="/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css"/>
		<script src="/js/jquery.min.js"></script>
		<script src="/bootstrap/js/bootstrap.min.js"></script>
	</head>
	<body>
		<div class="col-sm-8 col-sm-push-1">
			<p>
				Hello, ${this.getString('name')!''}!
			</p>

			<form method="post">
				<div class="form-group">
					<label for="name">You name:</label>
					<input type="text" class="form-control" id="name" placeholder="You name" name="name">
				</div>
				<button type="submit" class="btn btn-info">
					OK
				</button>
			</form>
			<pre>
&lt;html&gt;
  &lt;head&gt;
    &lt;title>This is a Velocity Template HTML&lt;/title&gt;
	&lt;link href="/bootstrap/css/bootstrap.min.css" rel="stylesheet" type="text/css"/&gt;
	&lt;script src="/js/jquery.min.js"&gt;&lt;/script&gt;
	&lt;script src="/bootstrap/js/bootstrap.min.js"&gt;&lt;/script&gt;
  &lt;/head&gt;
  &lt;body&gt;
		&lt;div class="col-sm-8 col-sm-push-1"&gt;
			&lt;p&gt;
				Hello, &amp;{this.getString('name')!''}!
			&lt;/p&gt;

			&lt;form method="post"&gt;
				&lt;div class="form-group"&gt;
					&lt;label for="name"&gt;You name:&lt;/label&gt;
					&lt;input type="text" class="form-control" id="name" placeholder="You name" name="name"&gt;
				&lt;/div>
				&lt;button type="submit" class="btn btn-info"&gt;
					OK
				&lt;/button&gt;
			&lt;/form&gt;
  &lt;/body&gt;
&lt;/html&gt;
</pre>
		</div>

	</body>
</html>

