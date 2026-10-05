package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * ПП ДЗ3
 * Множество Мандельброта
 * Для каждого пикселя экрана определяем входит ли соответствующее ему комплексное число c в множество Мандельброта
 * берём z = 0 и повторяем
 * z = z*z + c если |z| стало больше 2 число улетело в бесконечность и в множество не входит
 */
public class Main {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int MAX_ITERATIONS = 1000;

    // Область комплексной плоскости
    private static final double RE_MIN = -2.5;
    private static final double RE_MAX = 1.0;
    private static final double IM_MIN = -1.0;
    private static final double IM_MAX = 1.0;
    private static boolean inSet(int px, int py) {
        double cr = RE_MIN + (px + 0.5) * (RE_MAX - RE_MIN) / WIDTH;
        double ci = IM_MIN + (py + 0.5) * (IM_MAX - IM_MIN) / HEIGHT;

        double zr = 0;
        double zi = 0;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double zr2 = zr * zr;
            double zi2 = zi * zi;
            if (zr2 + zi2 > 4.0) {
                return false;
            }
            zi = 2 * zr * zi + ci;
            zr = zr2 - zi2 + cr;
        }
        return true;
    }

    // Многопоточный расчёт
    private static boolean[][] computeParallel(int threads) throws Exception {
        boolean[][] result = new boolean[HEIGHT][WIDTH];
        AtomicInteger nextRow = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    int row;
                    while ((row = nextRow.getAndIncrement()) < HEIGHT) {
                        for (int x = 0; x < WIDTH; x++) {
                            result[row][x] = inSet(x, row);
                        }
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }
        return result;
    }

    // Однопоточный расчёт
    private static boolean[][] computeSequential() {
        boolean[][] result = new boolean[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                result[y][x] = inSet(x, y);
            }
        }
        return result;
    }

    private static long count(boolean[][] grid) {
        long total = 0;
        for (boolean[] row : grid) {
            for (boolean in : row) {
                if (in) {
                    total++;
                }
            }
        }
        return total;
    }

    public static void main(String[] args) throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Размер: " + WIDTH + "x" + HEIGHT
                + " пикселей, максимум итераций: " + MAX_ITERATIONS
                + ", потоков: " + threads);

        long start = System.nanoTime();
        boolean[][] parallel = computeParallel(threads);
        long parallelMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        boolean[][] sequential = computeSequential();
        long sequentialMs = (System.nanoTime() - start) / 1_000_000;

        for (int y = 0; y < HEIGHT; y++) {
            if (!java.util.Arrays.equals(parallel[y], sequential[y])) {
                throw new AssertionError("Результаты отличаются в строке " + y);
            }
        }

        long inside = count(parallel);
        long total = (long) WIDTH * HEIGHT;
        System.out.println("Входит в множество: " + inside + " из " + total + " пикселей");
        System.out.println("Многопоточно:  " + parallelMs + " мс");
        System.out.println("Однопоточно:   " + sequentialMs + " мс");
    }
}