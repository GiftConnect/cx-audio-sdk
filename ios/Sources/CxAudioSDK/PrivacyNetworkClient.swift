import Foundation

internal final class PrivacyNetworkClient {
    static let shared = PrivacyNetworkClient()
    private init() {}

    func data(
        request: URLRequest,
        purpose: PrivacyRequestPurpose,
        completion: @escaping (Data?, URLResponse?, Error?) -> Void
    ) {
        do {
            let enforced = try enforce(request, purpose: purpose)
            let delegate = RedirectDelegate(purpose: purpose)
            let configuration = URLSessionConfiguration.ephemeral
            configuration.httpCookieStorage = nil
            configuration.urlCache = nil
            configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
            var session: URLSession!
            session = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)
            session.dataTask(with: enforced) { data, response, error in
                completion(data, response, error)
                session.finishTasksAndInvalidate()
                session = nil
            }.resume()
        } catch {
            completion(nil, nil, error)
        }
    }

    func download(
        request: URLRequest,
        purpose: PrivacyRequestPurpose,
        completion: @escaping (Result<URL, Error>) -> Void
    ) {
        do {
            let enforced = try enforce(request, purpose: purpose)
            let delegate = RedirectDelegate(purpose: purpose)
            let configuration = URLSessionConfiguration.ephemeral
            configuration.httpCookieStorage = nil
            configuration.urlCache = nil
            var session: URLSession!
            session = URLSession(configuration: configuration, delegate: delegate, delegateQueue: nil)
            session.downloadTask(with: enforced) { temporaryURL, response, error in
                defer {
                    session.finishTasksAndInvalidate()
                    session = nil
                }
                if let error { completion(.failure(error)); return }
                if let response = response as? HTTPURLResponse,
                   !(200..<300).contains(response.statusCode) {
                    completion(.failure(CxAudioError.httpStatus(
                        response.statusCode,
                        response.url ?? enforced.url!
                    )))
                    return
                }
                guard let temporaryURL else {
                    completion(.failure(CxAudioError.invalidOption("Creative download was empty")))
                    return
                }
                let extensionValue = enforced.url?.pathExtension ?? ""
                let destination = FileManager.default.temporaryDirectory
                    .appendingPathComponent("cx-audio-\(UUID().uuidString)")
                    .appendingPathExtension(extensionValue)
                do {
                    try FileManager.default.moveItem(at: temporaryURL, to: destination)
                    completion(.success(destination))
                } catch {
                    completion(.failure(error))
                }
            }.resume()
        } catch {
            completion(.failure(error))
        }
    }

    private func enforce(_ request: URLRequest, purpose: PrivacyRequestPurpose) throws -> URLRequest {
        guard let url = request.url else { throw CxAudioError.invalidURL("") }
        var result = request
        result.url = try PrivacyEnforcer.enforce(url, purpose: purpose)
        result.setValue(nil, forHTTPHeaderField: "Cookie")
        return result
    }
}

private final class RedirectDelegate: NSObject, URLSessionTaskDelegate {
    private let purpose: PrivacyRequestPurpose
    init(purpose: PrivacyRequestPurpose) { self.purpose = purpose }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        guard let url = request.url,
              let enforcedURL = try? PrivacyEnforcer.enforce(url, purpose: purpose) else {
            completionHandler(nil)
            return
        }
        var enforced = request
        enforced.url = enforcedURL
        enforced.setValue(nil, forHTTPHeaderField: "Cookie")
        completionHandler(enforced)
    }
}
