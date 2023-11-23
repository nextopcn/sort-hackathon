/*
 * Copyright 2016-2017 Leon Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.nextop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Baoyi Chen
 */
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 8, time = 2, timeUnit = TimeUnit.SECONDS)
public class MainBenchmark {
	
	@State(Scope.Benchmark)
	public static class Sort {
		
		private List<Integer[]> v;
		private Random random = ThreadLocalRandom.current();
		private Comparator<Integer> asc = Integer::compareTo;
		
		@Setup(Level.Invocation)
		public void setup() {
			this.v = new ArrayList<>(32);
			for (int i = 0; i < 32; i++) {
				Integer[] v = new Integer[16];
				for (int j = 0; j < 16; j++) {
					v[j] = random.nextInt();
				}
				this.v.add(v);
			}
		}
	}
	
	@Benchmark
	public List<Integer[]> sort(Sort sort) {
		Main.sort(sort.v, sort.asc);
		return sort.v;
	}
	
	@Benchmark
	public List<Integer[]> baseline(Sort sort) {
		Main.baseline(sort.v, sort.asc);
		return sort.v;
	}
	
	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include(MainBenchmark.class.getSimpleName())
				.shouldDoGC(true)
				.build();
		new Runner(opt).run();
	}
}
