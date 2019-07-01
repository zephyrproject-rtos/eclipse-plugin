# ===== DO NOT REMOVE =====
cmake_minimum_required(VERSION 3.13.1)
include($ENV{ZEPHYR_BASE}/cmake/app/boilerplate.cmake NO_POLICY_SCOPE)
# ===== DO NOT REMOVE =====

project(${projectName})

target_sources(app PRIVATE src/main.c)
