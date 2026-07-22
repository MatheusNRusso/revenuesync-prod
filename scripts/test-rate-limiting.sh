#!/bin/bash

# =============================================================================
# RevenueSync - Block 9 Task 1: Rate Limiting Validation Script
# =============================================================================

BASE_URL="http://localhost:8080"
CURL_TIMEOUT="--max-time 5 --connect-timeout 3"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}  RevenueSync - Block 9 Task 1: Rate Limiting Validation${NC}"
echo -e "${BLUE}======================================================================${NC}"
echo ""

http_get() {
    curl -s -o /dev/null -w "%{http_code}" $CURL_TIMEOUT "$1" 2>/dev/null || echo "000"
}

http_post() {
    curl -s -o /dev/null -w "%{http_code}" $CURL_TIMEOUT -X POST "$1" \
        -H "Content-Type: application/json" \
        -d "$2" 2>/dev/null || echo "000"
}

# ---------------------------------------------------------------------------
check_app_running() {
    echo -e "${YELLOW}[INFO] Checking if application is running...${NC}"
    local code
    code=$(http_get "${BASE_URL}/actuator/health")
    if [ "$code" = "200" ]; then
        echo -e "${GREEN}[OK] Application is running at ${BASE_URL}${NC}"
        echo ""
    else
        echo -e "${RED}[FAIL] Application not responding at ${BASE_URL} (HTTP $code)${NC}"
        echo -e "${YELLOW}[INFO] Start the application with: mvn spring-boot:run${NC}"
        exit 1
    fi
}

# ---------------------------------------------------------------------------
test_health_check() {
    echo -e "${BLUE}--- Test 1: Health Check (no rate limit) ---${NC}"
    echo "Sending 15 rapid requests to /actuator/health"

    local success_count=0
    for i in $(seq 1 15); do
        local code
        code=$(http_get "${BASE_URL}/actuator/health")
        if [ "$code" = "200" ]; then
            success_count=$((success_count + 1))
        fi
        echo "  Request $i: $code"
    done

    if [ "$success_count" -eq 15 ]; then
        echo -e "${GREEN}[OK] Health Check: All 15 requests returned 200${NC}"
    else
        echo -e "${RED}[FAIL] Health Check: Only $success_count/15 returned 200${NC}"
    fi
    echo ""
}

# ---------------------------------------------------------------------------
test_public_endpoints() {
    echo -e "${BLUE}--- Test 2: Public Endpoints (limit: 100 req/min) ---${NC}"
    echo "Sending 20 rapid requests to public endpoint"

    local endpoints=(
        "/api/public/merchants"
        "/api/public/profiles"
        "/api/solana/status"
    )

    local test_endpoint=""
    for ep in "${endpoints[@]}"; do
        local code
        code=$(http_get "${BASE_URL}${ep}")
        if [ "$code" != "000" ] && [ "$code" != "404" ]; then
            test_endpoint="$ep"
            break
        fi
    done

    if [ -z "$test_endpoint" ]; then
        echo -e "${YELLOW}[WARN] No public endpoint found, using /actuator/health${NC}"
        test_endpoint="/actuator/health"
    fi

    echo "Selected endpoint: $test_endpoint"
    echo ""

    local success_count=0
    local rate_limited_count=0
    for i in $(seq 1 20); do
        local code
        code=$(http_get "${BASE_URL}${test_endpoint}")
        if [ "$code" = "200" ] || [ "$code" = "401" ] || [ "$code" = "403" ] || [ "$code" = "404" ]; then
            success_count=$((success_count + 1))
            echo -e "  Request $i: ${code} ${GREEN}(OK)${NC}"
        elif [ "$code" = "429" ]; then
            rate_limited_count=$((rate_limited_count + 1))
            echo -e "  Request $i: ${code} ${RED}(Rate Limited)${NC}"
        else
            echo -e "  Request $i: ${code} ${YELLOW}(Unexpected)${NC}"
        fi
    done

    echo ""
    if [ "$rate_limited_count" -eq 0 ]; then
        echo -e "${GREEN}[OK] Public Endpoints: No requests blocked (expected for 20 req)${NC}"
    else
        echo -e "${YELLOW}[WARN] Public Endpoints: $rate_limited_count requests blocked${NC}"
    fi
    echo ""
}

# ---------------------------------------------------------------------------
test_auth_endpoints() {
    echo -e "${BLUE}--- Test 3: Auth Endpoints (limit: 5 req/min) ---${NC}"
    echo "Sending 8 rapid requests to /auth/login"
    echo "Expected: First 5 return 401, next 3 return 429"
    echo ""

    local success_count=0
    local rate_limited_count=0
    local first_429_at=0

    for i in $(seq 1 8); do
        local code
        code=$(http_post "${BASE_URL}/auth/login" '{"email":"test@test.com","password":"test"}')

        if [ "$code" = "429" ]; then
            rate_limited_count=$((rate_limited_count + 1))
            if [ "$first_429_at" -eq 0 ]; then
                first_429_at=$i
            fi
            echo -e "  Request $i: ${code} ${RED}(Rate Limited)${NC}"
        elif [ "$code" = "200" ] || [ "$code" = "401" ] || [ "$code" = "403" ] || [ "$code" = "400" ]; then
            success_count=$((success_count + 1))
            echo -e "  Request $i: ${code} ${GREEN}(OK)${NC}"
        else
            echo -e "  Request $i: ${code} ${YELLOW}(Unexpected)${NC}"
        fi
    done

    echo ""
    if [ "$first_429_at" -gt 0 ] && [ "$first_429_at" -le 6 ]; then
        echo -e "${GREEN}[OK] Auth Endpoints: Rate limiting triggered at request $first_429_at${NC}"
        echo -e "${GREEN}       Total: $success_count success, $rate_limited_count blocked${NC}"
    elif [ "$rate_limited_count" -gt 0 ]; then
        echo -e "${YELLOW}[WARN] Auth Endpoints: Rate limiting triggered, but not at expected request${NC}"
        echo -e "       First 429 at request $first_429_at (expected between 5-6)"
    else
        echo -e "${RED}[FAIL] Auth Endpoints: No requests were rate limited${NC}"
        echo -e "       Check if the filter is registered correctly${NC}"
    fi
    echo ""
}

# ---------------------------------------------------------------------------
test_429_response_format() {
    echo -e "${BLUE}--- Test 4: 429 Response Format ---${NC}"

    # Exhaust the limit
    for i in $(seq 1 6); do
        http_post "${BASE_URL}/auth/login" '{"email":"test@test.com","password":"test"}' > /dev/null
    done

    echo "Fetching detailed 429 response..."
    local response
    response=$(curl -s $CURL_TIMEOUT -X POST "${BASE_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"test@test.com","password":"test"}' 2>/dev/null)

    echo "Response received:"
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    echo ""

    if echo "$response" | grep -q "error"; then
        echo -e "${GREEN}[OK] 429 Response: JSON contains 'error' field${NC}"
    else
        echo -e "${RED}[FAIL] 429 Response: JSON missing 'error' field${NC}"
    fi
    echo ""
}

# ---------------------------------------------------------------------------
test_429_headers() {
    echo -e "${BLUE}--- Test 5: 429 Response Headers ---${NC}"

    # Exhaust the limit again
    for i in $(seq 1 6); do
        http_post "${BASE_URL}/auth/login" '{"email":"test@test.com","password":"test"}' > /dev/null
    done

    # Use -D - to dump headers from POST request
    local headers
    headers=$(curl -s -D - -o /dev/null $CURL_TIMEOUT -X POST "${BASE_URL}/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"test@test.com","password":"test"}' 2>&1)

    echo "Headers received:"
    echo "$headers"
    echo ""

    if echo "$headers" | grep -q "429"; then
        echo -e "${GREEN}[OK] Headers: Status 429 present${NC}"
    else
        echo -e "${RED}[FAIL] Headers: Status 429 not found${NC}"
    fi

    if echo "$headers" | grep -qi "content-type.*json"; then
        echo -e "${GREEN}[OK] Headers: Content-Type application/json present${NC}"
    else
        echo -e "${YELLOW}[WARN] Headers: Content-Type may not be set as JSON${NC}"
    fi
    echo ""
}

# ---------------------------------------------------------------------------
test_regression() {
    echo -e "${BLUE}--- Test 6: Regression Check ---${NC}"
    echo "Testing critical endpoints to ensure nothing is broken..."
    echo ""

    local health_code
    health_code=$(http_get "${BASE_URL}/actuator/health")
    if [ "$health_code" = "200" ]; then
        echo -e "  Health Check: ${GREEN}[OK]${NC}"
    else
        echo -e "  Health Check: ${RED}[FAIL]${NC} (HTTP $health_code)"
    fi

    local info_code
    info_code=$(http_get "${BASE_URL}/actuator/info")
    if [ "$info_code" = "200" ]; then
        echo -e "  Actuator Info: ${GREEN}[OK]${NC}"
    else
        echo -e "  Actuator Info: ${YELLOW}[WARN]${NC} (HTTP $info_code)"
    fi

    local merchants_code
    merchants_code=$(http_get "${BASE_URL}/api/public/merchants")
    if [ "$merchants_code" = "200" ]; then
        echo -e "  Public Merchants: ${GREEN}[OK]${NC}"
    else
        echo -e "  Public Merchants: ${YELLOW}[WARN]${NC} (HTTP $merchants_code)"
    fi

    echo ""
    echo -e "${GREEN}[OK] Regression check completed${NC}"
    echo ""
}

# ---------------------------------------------------------------------------
print_summary() {
    echo -e "${BLUE}======================================================================${NC}"
    echo -e "${BLUE}  TEST SUMMARY${NC}"
    echo -e "${BLUE}======================================================================${NC}"
    echo ""
    echo "  Tests executed:"
    echo "   1. Health Check (no rate limit)"
    echo "   2. Public Endpoints (limit 100/min)"
    echo "   3. Auth Endpoints (limit 5/min)"
    echo "   4. 429 Response Format"
    echo "   5. 429 Response Headers"
    echo "   6. Regression Check"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo "   1. If all tests passed: commit changes"
    echo "   2. If any test failed: debug and fix"
    echo "   3. After approval: proceed to Task 2 (Security Headers)"
    echo ""
    echo -e "${BLUE}======================================================================${NC}"
}

# =============================================================================
# Main execution
# =============================================================================

check_app_running
test_health_check
test_public_endpoints
test_auth_endpoints
test_429_response_format
test_429_headers
test_regression
print_summary

echo -e "${GREEN}All tests executed!${NC}"
