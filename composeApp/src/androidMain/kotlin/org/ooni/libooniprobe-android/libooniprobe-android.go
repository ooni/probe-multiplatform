package main

// #cgo LDFLAGS: -llog
// #include <android/log.h>

import "C"
import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
)

/** BEGIN API section **/
// In reality the code in here should live inside of probe-cli
// It represents the API contract that exists between OONI Probe CLI and the apps.

// pkg/ooniprobe/api/
func DoHTTPRequest(url string, retryCount int) (string, error) {
	fmt.Printf("we don't actually implement %d retries\n", retryCount)
	response, err := http.Get(url)
	if err != nil {
		return "", fmt.Errorf("failed to perform request: %v", err)
	}
	defer response.Body.Close()

	resp, err := io.ReadAll(response.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response: %v", err)
	}

	return string(resp), nil
}

func GetPublicIP() (string, error) {
	return DoHTTPRequest("https://api.ipify.org", 1)
}

// pkg/ooniprobe/mobileapi/
var ErrUnknownFuncName = errors.New("invalid function name")

// Any represents a value of any type.
type Any interface{}

// ParseAnyList parses a JSON string into a list of Any.
func ParseAnyList(s string) ([]Any, error) {
	var data []Any
	err := json.Unmarshal([]byte(s), &data)
	if err != nil {
		return nil, err
	}
	return data, nil
}

type API struct {
}

func (a API) Init() {
	// Do any initialization needed to get the API running
}

func (a API) Call(funcName string) (string, error) {
	switch funcName {
	case "GetPublicIP":
		return GetPublicIP()
	default:
		return "", ErrUnknownFuncName
	}
}

type returnValueHTTPResponse struct {
	Body string `json:"body"`
}

func (a API) CallWithArgs(funcName string, args []Any) (string, error) {
	switch funcName {
	case "DoHTTPRequest":
		if len(args) != 2 {
			return "", errors.New("DoHTTPRequest takes exactly 2 arguments")
		}

		url, ok := args[0].(string)
		if !ok {
			return "", errors.New("DoHTTPRequest: args[0](name) must be a string")
		}

		retryCount, ok := args[1].(float64)
		if !ok {
			return "", errors.New("DoHTTPRequest: args[1](count) must be a number")
		}
		body, err := DoHTTPRequest(url, int(retryCount))
		if err != nil {
			return "", err
		}

		ret, err := json.Marshal(returnValueHTTPResponse{Body: body})
		return string(ret), err
	default:
		return "", fmt.Errorf("%s: %v", funcName, ErrUnknownFuncName)
	}
}

/** END API section **/

var api API

type ReturnValue struct {
	Value interface{} `json:"return_value"`
	Err   interface{} `json:"error"`
}

func SerializeReturnValue(v string, err error) *C.char {
	var errVal interface{}
	if err != nil {
		errVal = fmt.Sprintf("%v", err)
	}
	rv := ReturnValue{
		Value: v,
		Err:   errVal,
	}
	b, err := json.Marshal(rv)
	if err != nil {
		return nil
	}
	return C.CString(string(b))
}

func init() {
	api = API{}
	api.Init()
}

//export apiCall
func apiCall(funcName string) *C.char {
	rv, err := api.Call(funcName)
	return SerializeReturnValue(rv, err)
}

//export apiCallWithArgs
func apiCallWithArgs(funcName string, argsJSON string) *C.char {
	args, err := ParseAnyList(argsJSON)
	if err != nil {
		return SerializeReturnValue("", err)
	}
	rv, err := api.CallWithArgs(funcName, args)
	return SerializeReturnValue(rv, err)
}

func main() {}
