<script lang="ts">
	import Runner from "../api/runner.ts";

	let running = false;
	let data = {
		username: "",
		password: "",

		algorithm_file: "test_algorithm.py",
		data_file: "test_data.txt",
	};
	let output = null;

	async function handleClick() {
		output = null;
		running = true;

		try {
			let { data: response } = await Runner.start(data);
			console.log(response);
			output = response.output;
		} catch (error) {
			output = error.response ? error.response.data : error.toString();
		}

		running = false;
		return false;
	}
</script>

<svelte:head>
    <title>DEX</title>
</svelte:head>

<div class="container">
	<div class="row">
		<div class="col-xs-12 col-md-4">
			<form>
				<div class="form-group">
					<label for="username">
						Username:
						<input
							class="form-control"
							id="username"
							bind:value={data.username}
						>
					</label>
				</div>

				<div class="form-group">
					<label for="password">
						Password:
						<input
							class="form-control"
							id="password"
							type="password"
							bind:value={data.password}
						>
					</label>
				</div>

				<div class="form-group">
					<label for="algorithm-file">
						Algorithm file:
						<input
							class="form-control"
							id="algorithm-file"
							bind:value={data.algorithm_file}
						>
					</label>
				</div>

				<div class="form-group">
					<label for="data-file">
						Data file:
						<input
							class="form-control"
							id="data-file"
							bind:value={data.data_file}
						>
					</label>
				</div>

				<div class="form-group">
					<a
						href="#0"
						class="form-control btn btn-primary"
						disabled={running}
						on:click={handleClick}
					>
						{running ? "Running..." : "Run!"}
					</a>
				</div>
			</form>
		</div>

		<div class="col-xs-12 col-md-8">
			<pre>
				{output || "No output (yet)…"}
			</pre>
		</div>
	</div>
</div>
